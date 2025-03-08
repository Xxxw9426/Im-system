package com.lld.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.lld.im.service.friendship.dao.ImFriendShipGroupMemberEntity;
import com.lld.im.service.friendship.dao.mapper.ImFriendShipGroupMemberMapper;
import com.lld.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.lld.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.lld.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.lld.im.service.friendship.service.ImFriendShipGroupService;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-06
 * @Description: 分组成员模块的业务逻辑实现类
 * @Version: 1.0
 */

@Service
public class ImFriendShipGroupMemberServiceImpl implements ImFriendShipGroupMemberService {

    @Autowired
    ImFriendShipGroupService imFriendShipGroupService;


    @Autowired
    ImUserService imUserService;


    @Autowired
    ImFriendShipGroupMemberMapper imFriendShipGroupMemberMapper;

    /***
     * 向分组内添加成员
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO addGroupMember(AddFriendShipGroupMemberReq req) {
        // 首先根据拥有分组的用户id和组名查询是否存在分组
        ResponseVO<ImFriendShipGroupEntity> group=imFriendShipGroupService.getGroup(req.getFromId(),req.getGroupName(),req.getAppId());
        // 分组不存在
        if(!group.isOk())  {
            return group;
        }
        // 存在的话，获取要存入分组的用户id并且插入数据库
        // 并且最终返回成功加入分组的用户id
        List<String> successId =new ArrayList<>();
        for(String id:req.getToIds()) {
            // 插入之前判断要加入分组的用户是否存在
            ResponseVO<ImUserDataEntity> userInfo = imUserService.getSingleUserInfo(id, req.getAppId());
            // 用户存在，加入分组
            if(userInfo.isOk())  {
                int i=doAddGroupMember(group.getData().getGroupId(),id);
                if(i==1) {
                    successId.add(id);
                }
            }
        }
        return ResponseVO.successResponse(successId);
    }


    /***
     * 向数据库中组成员的表内插入用户
     * @param groupId
     * @param id
     * @return
     */
    @Override
    public int doAddGroupMember(Long groupId, String id) {
        ImFriendShipGroupMemberEntity groupMember=new ImFriendShipGroupMemberEntity();
        groupMember.setGroupId(groupId);
        groupMember.setToId(id);
        try{
            int insert = imFriendShipGroupMemberMapper.insert(groupMember);
            return insert;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    /***
     * 根据分组id清空当前分组的所有用户
     * @param groupId
     */
    @Override
    public int clearGroupMember(Long groupId) {
        QueryWrapper<ImFriendShipGroupMemberEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("group_id",groupId);
        int delete = imFriendShipGroupMemberMapper.delete(queryWrapper);
        return delete;
    }


    /***
     * 删除分组中指定的用户集
     * @param req
     * @return
     */
    @Override
    public ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req) {
        // 调用groupService中的方法判断当前组是否存在
        ResponseVO<ImFriendShipGroupEntity> group=imFriendShipGroupService.getGroup(req.getFromId(),req.getGroupName(),req.getAppId());
        if(!group.isOk())  {
            return group;
        }
        ArrayList success = new ArrayList();
        // 获取要删除的所有用户的id
        for (String toId : req.getToIds()) {
            // 判断当前用户是否存在
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            // 存在，调用删除方法
            if(singleUserInfo.isOk()){
                int i = deleteGroupMember(group.getData().getGroupId(), toId);
                // 删除成功则加入结果集
                if(i == 1){
                    success.add(toId);
                }
            }
        }
        return ResponseVO.successResponse(success);
    }


    /***
     * 业务删除群成员
     * @param groupId
     * @param toId
     * @return
     */
    public int deleteGroupMember(Long groupId, String toId) {
        QueryWrapper<ImFriendShipGroupMemberEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id",groupId);
        queryWrapper.eq("to_id",toId);

        try {
            int delete = imFriendShipGroupMemberMapper.delete(queryWrapper);
            return delete;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }


}
