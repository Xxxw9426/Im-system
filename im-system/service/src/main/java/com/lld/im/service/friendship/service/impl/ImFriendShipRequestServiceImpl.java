package com.lld.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.codec.pack.friendship.ApproverFriendRequestPack;
import com.lld.im.codec.pack.friendship.ReadAllFriendRequestPack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ApproveFriendRequestStatusEnum;
import com.lld.im.common.enums.FriendShipErrorCode;
import com.lld.im.common.enums.command.FriendshipEventCommand;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.service.friendship.dao.ImFriendShipRequestEntity;
import com.lld.im.service.friendship.dao.mapper.ImFriendShipRequestMapper;
import com.lld.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.lld.im.service.friendship.model.req.FriendDto;
import com.lld.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.lld.im.service.friendship.service.ImFriendShipRequestService;
import com.lld.im.service.friendship.service.ImFriendShipService;
import com.lld.im.service.seq.RedisSeq;
import com.lld.im.service.utils.MessageProducer;
import com.lld.im.service.utils.WriteUserSeq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.scanner.Constant;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-05
 * @Description: 好友申请模块的业务逻辑实现类
 * @Version: 1.0
 */

@Service
public class ImFriendShipRequestServiceImpl implements ImFriendShipRequestService {

    @Autowired
    ImFriendShipRequestMapper imFriendShipRequestMapper;


    @Autowired
    ImFriendShipService imFriendShipService;


    @Autowired
    MessageProducer messageProducer;


    @Autowired
    RedisSeq redisSeq;


    @Autowired
    WriteUserSeq writeUserSeq;


    /***
     *  插入一条好友申请
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    @Override
    public ResponseVO addFriendShipRequest(String fromId, FriendDto dto, Integer appId) {

        // 先查询一下表中是否已经有好友申请
        QueryWrapper<ImFriendShipRequestEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("from_id", fromId);
        queryWrapper.eq("app_id", appId);
        queryWrapper.eq("to_id", dto.getToId());
        // 查询
        ImFriendShipRequestEntity entity = imFriendShipRequestMapper.selectOne(queryWrapper);
        Long seq= redisSeq.doGetSeq(appId+":"+ Constants.SeqConstants.FriendshipRequest);
        // 记录不存在
        if(entity == null) {
            // 插入当前好友申请
            entity = new ImFriendShipRequestEntity();
            entity.setAppId(appId);
            entity.setSequence(seq);
            entity.setFromId(fromId);
            entity.setToId(dto.getToId());
            // 设置添加来源
            entity.setAddSource(dto.getAddSource());
            // 设置添加留言
            entity.setAddWording(dto.getAddWording());
            // 设置好友申请已读状态
            entity.setReadStatus(0);
            // 设置好友申请通过状态
            entity.setApproveStatus(0);
            // 设置备注
            entity.setRemark(dto.getRemark());
            // 设置创建时间
            entity.setCreateTime(System.currentTimeMillis());
            // 插入
            imFriendShipRequestMapper.insert(entity);
            // 记录存在
        } else {
            // 更新记录中的内容
            if(StringUtils.isNoneBlank(dto.getAddSource())) {
                entity.setAddSource(dto.getAddSource());
            }
            if(StringUtils.isNoneBlank(dto.getRemark())) {
                entity.setRemark(dto.getRemark());
            }
            if(StringUtils.isNoneBlank(dto.getAddWording())) {
                entity.setAddWording(dto.getAddWording());
            }
            entity.setSequence(seq);
            // 执行更新操作
            imFriendShipRequestMapper.updateById(entity);
        }
        writeUserSeq.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FriendshipRequest,seq);
        // 发送好友申请后发送tcp通知
        // 这里需要注意的是：这里我们不是给发送好友申请的客户端发送tcp通知，而是给接收好友申请的所有客户端发送tcp通知
        messageProducer.sendToUser(dto.getToId(),
                null, "", FriendshipEventCommand.FRIEND_REQUEST,
                entity, appId);

        return ResponseVO.successResponse();

    }


    /***
     * 审批好友申请
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO approveFriendRequest(ApproveFriendRequestReq req) {

        // 根据主键Id查询好友申请消息
        ImFriendShipRequestEntity entity = imFriendShipRequestMapper.selectById(req.getId());
        // 好友申请不存在
        if(entity == null) {
            throw new ApplicationException(FriendShipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }
        // 如果好友申请的主角不是当前用户(即用户只能审批发送给自己的好友申请)
        if(!req.getOperator().equals(entity.getToId())) {
            throw new ApplicationException(FriendShipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }
        Long seq= redisSeq.doGetSeq(req.getAppId()+":"+ Constants.SeqConstants.FriendshipRequest);
        // 审批好友申请
        ImFriendShipRequestEntity update=new ImFriendShipRequestEntity();
        // 修改同意状态
        update.setApproveStatus(req.getStatus());
        // 修改更新时间
        update.setUpdateTime(System.currentTimeMillis());
        // 根据id修改
        update.setId(req.getId());
        update.setSequence(seq);
        imFriendShipRequestMapper.updateById(update);
        writeUserSeq.writeUserSeq(req.getAppId(),req.getOperator(), Constants.SeqConstants.FriendshipRequest,seq);

        // 如果用户审批通过 ===> 执行添加好友逻辑
        if(ApproveFriendRequestStatusEnum.AGREE.getCode()==req.getStatus()) {
            FriendDto dto=new FriendDto();
            dto.setToId(entity.getToId());
            dto.setRemark(entity.getRemark());
            dto.setAddSource(entity.getAddSource());
            dto.setAddWording(entity.getAddWording());
            // 添加好友
            ResponseVO responseVO = imFriendShipService.doAddFriend(req,entity.getFromId(), dto, req.getAppId());
            // 如果返回的结果不是添加成功并且也不是对方已经是你的好友
            if(! responseVO.isOk() && responseVO.getCode()!=FriendShipErrorCode.TO_IS_YOUR_FRIEND.getCode()) {
                return responseVO;
            }
        }

        // 审批好友申请后发送tcp通知
        ApproverFriendRequestPack pack=new ApproverFriendRequestPack();
        pack.setId(req.getId());
        pack.setStatus(req.getStatus());
        pack.setSequence(seq);
        messageProducer.sendToUser(entity.getToId(),req.getClientType(),req.getImei(), FriendshipEventCommand
                .FRIEND_REQUEST_APPROVE,pack,req.getAppId());

        return ResponseVO.successResponse();
    }


    /***
     * 已读好友申请列表
     * @param req
     * @return
     */
    @Override
    public ResponseVO readFriendShipRequest(ReadFriendShipRequestReq req) {

        // 根据fromId和appId获取所有当前用户的好友申请列表
        QueryWrapper<ImFriendShipRequestEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("to_id", req.getFromId());
        queryWrapper.eq("app_id", req.getAppId());

        Long seq= redisSeq.doGetSeq(req.getAppId()+":"+ Constants.SeqConstants.FriendshipRequest);
        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        // 设置好友申请阅读状态为已读
        update.setReadStatus(1);
        update.setSequence(seq);
        // 更新所有查询到的好友申请阅读状态为已读
        imFriendShipRequestMapper.update(update, queryWrapper);
        writeUserSeq.writeUserSeq(req.getAppId(),req.getOperator(), Constants.SeqConstants.FriendshipRequest,seq);

        // 更新完已读状态后发送tcp通知
        ReadAllFriendRequestPack pack=new ReadAllFriendRequestPack();
        pack.setFromId(req.getFromId());
        pack.setSequence(seq);
        messageProducer.sendToUser(req.getFromId(),req.getClientType(),req.getImei(),FriendshipEventCommand
                .FRIEND_REQUEST_READ,pack,req.getAppId());

        return ResponseVO.successResponse();
    }


    /***
     * 获取好友申请列表
     * @param fromId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO getFriendRequest(String fromId, Integer appId) {

        QueryWrapper<ImFriendShipRequestEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("to_id", fromId);
        queryWrapper.eq("app_id", appId);

        // 根据fromId和appId查询当前用户的所有好友申请列表
        List<ImFriendShipRequestEntity> requestList = imFriendShipRequestMapper.selectList(queryWrapper);
        return ResponseVO.successResponse(requestList);
    }
}
