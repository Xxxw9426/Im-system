package com.lld.im.service.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.codec.pack.friendship.AddFriendGroupPack;
import com.lld.im.codec.pack.friendship.DeleteFriendGroupPack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.DelFlagEnum;
import com.lld.im.common.enums.FriendShipErrorCode;
import com.lld.im.common.enums.command.FriendshipEventCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.lld.im.service.friendship.dao.mapper.ImFriendShipGroupMapper;
import com.lld.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.lld.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.lld.im.service.friendship.model.req.DeleteFriendShipGroupReq;
import com.lld.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.lld.im.service.friendship.service.ImFriendShipGroupService;
import com.lld.im.service.seq.RedisSeq;
import com.lld.im.service.utils.MessageProducer;
import com.lld.im.service.utils.WriteUserSeq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-06
 * @Description: 好友分组模块的业务逻辑实现类
 * @Version: 1.0
 */

@Service
public class ImFriendShipGroupServiceImpl implements ImFriendShipGroupService {

    @Autowired
    ImFriendShipGroupMapper imFriendShipGroupMapper;


    @Autowired
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;


    @Autowired
    MessageProducer messageProducer;


    @Autowired
    RedisSeq redisSeq;


    @Autowired
    WriteUserSeq writeUserSeq;


    /***
     *  创建好友分组
     * @param req
     * @return
     */
    @Override
    public ResponseVO addGroup(AddFriendShipGroupReq req) {
        // 根据用户传入数据查询数据库中是否已经有这个分组
        QueryWrapper<ImFriendShipGroupEntity> query=new QueryWrapper<>();
        query.eq("from_id",req.getFromId());
        query.eq("group_name",req.getGroupName());
        query.eq("app_id",req.getAppId());
        ImFriendShipGroupEntity entity=imFriendShipGroupMapper.selectOne(query);
        // 如果分组已经存在则返回分组已存在错误信息
        if(entity!=null) {
            // 判断当前分组的状态
            if(entity.getDelFlag()==DelFlagEnum.DELETE.getCode()) {
                // 修改当前分组的删除状态为未删除
                ImFriendShipGroupEntity update=new ImFriendShipGroupEntity();
                update.setDelFlag(DelFlagEnum.NORMAL.getCode());
                imFriendShipGroupMapper.update(update,query);
                // 将用户传入的群成员加入数据库
                AddFriendShipGroupMemberReq addMember=new AddFriendShipGroupMemberReq();
                addMember.setFromId(req.getFromId());
                addMember.setToIds(req.getToIds());
                addMember.setAppId(req.getAppId());
                addMember.setGroupName(req.getGroupName());
                imFriendShipGroupMemberService.addGroupMember(addMember);
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }
        // 不存在的话，创建分组
        Long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
        ImFriendShipGroupEntity insert=new ImFriendShipGroupEntity();
        insert.setAppId(req.getAppId());
        insert.setSequence(seq);
        insert.setGroupName(req.getGroupName());
        insert.setFromId(req.getFromId());
        insert.setCreateTime(System.currentTimeMillis());
        insert.setDelFlag(DelFlagEnum.NORMAL.getCode());
        try{
            // 插入数据库
            int insert1=imFriendShipGroupMapper.insert(insert);
            // 插入失败
            if(insert1!=1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
            }
            // 插入成功并且用户传入了组内成员
            // 调用向分组内插入成员的方法插入成员
            if(insert1==1 && CollectionUtil.isNotEmpty(req.getToIds())) {
                AddFriendShipGroupMemberReq addMember=new AddFriendShipGroupMemberReq();
                addMember.setFromId(req.getFromId());
                addMember.setToIds(req.getToIds());
                addMember.setAppId(req.getAppId());
                addMember.setGroupName(req.getGroupName());
                imFriendShipGroupMemberService.addGroupMember(addMember);
                return ResponseVO.successResponse();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
        }

        // 创建好友分组成功后发送tcp通知
        AddFriendGroupPack pack=new AddFriendGroupPack();
        pack.setFromId(req.getFromId());
        pack.setGroupName(req.getGroupName());
        pack.setSequence(seq);
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_ADD,
                pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);

        return ResponseVO.successResponse();
    }


    /***
     * 根据组名和拥有者id获取组的信息
     * @param fromId
     * @param groupName
     * @param appId
     * @return
     */
    @Override
    public ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId) {
        // 根据组名和拥有者id,appId和删除状态查询分组
        QueryWrapper<ImFriendShipGroupEntity> query=new QueryWrapper<>();
        query.eq("from_id",fromId);
        query.eq("group_name",groupName);
        query.eq("app_id",appId);
        query.eq("del_flag",DelFlagEnum.NORMAL.getCode());
        ImFriendShipGroupEntity entity=imFriendShipGroupMapper.selectOne(query);
        if(entity==null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }


    /***
     * 删除好友分组并删除分组下的所有用户
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO deleteGroup(DeleteFriendShipGroupReq req) {
        // 首先获取当前用户要删除的所有分组
        for(String groupName:req.getGroupName()) {
            // 获取到每一个判断是否存在该分组
            QueryWrapper<ImFriendShipGroupEntity> query=new QueryWrapper<>();
            query.eq("group_name",groupName);
            query.eq("del_flag",DelFlagEnum.NORMAL.getCode());
            query.eq("from_id",req.getFromId());
            query.eq("app_id",req.getAppId());
            ImFriendShipGroupEntity entity=imFriendShipGroupMapper.selectOne(query);
            // 如果存在该分组则删除分组与分组下的所有成员
            if(entity!=null) {
                Long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
                // 更新数据库中当前分组的删除字段
                ImFriendShipGroupEntity update=new ImFriendShipGroupEntity();
                update.setGroupId(entity.getGroupId());
                update.setDelFlag(DelFlagEnum.DELETE.getCode());
                update.setSequence(seq);
                imFriendShipGroupMapper.updateById(update);
                // 调用memberService层的删除分组中的所有成员的办法删除所有成员
                imFriendShipGroupMemberService.clearGroupMember(entity.getGroupId());

                // 删除好友分组成功后发送tcp通知
                DeleteFriendGroupPack pack=new DeleteFriendGroupPack();
                pack.setSequence(seq);
                pack.setFromId(req.getFromId());
                pack.setGroupName(groupName);
                //TCP通知
                messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_DELETE,
                        pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
                writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);

            }
        }
        return ResponseVO.successResponse();
    }

}
