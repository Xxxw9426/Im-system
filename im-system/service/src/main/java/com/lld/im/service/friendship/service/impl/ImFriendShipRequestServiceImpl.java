package com.lld.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.ApproveFriendRequestStatusEnum;
import com.lld.im.common.enums.FriendShipErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.service.friendship.dao.ImFriendShipRequestEntity;
import com.lld.im.service.friendship.dao.mapper.ImFriendShipRequestMapper;
import com.lld.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.lld.im.service.friendship.model.req.FriendDto;
import com.lld.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.lld.im.service.friendship.service.ImFriendShipRequestService;
import com.lld.im.service.friendship.service.ImFriendShipService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 记录不存在
        if(entity == null) {
            // 插入当前好友申请
            entity = new ImFriendShipRequestEntity();
            entity.setAppId(appId);
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
            // 执行更新操作
            imFriendShipRequestMapper.updateById(entity);
        }
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
        // 审批好友申请
        ImFriendShipRequestEntity update=new ImFriendShipRequestEntity();
        // 修改同意状态
        update.setApproveStatus(req.getStatus());
        // 修改更新时间
        update.setUpdateTime(System.currentTimeMillis());
        // 根据id修改
        update.setId(req.getId());
        imFriendShipRequestMapper.updateById(update);

        // 如果用户审批通过 ===> 执行添加好友逻辑
        if(ApproveFriendRequestStatusEnum.AGREE.getCode()==req.getStatus()) {
            FriendDto dto=new FriendDto();
            dto.setToId(entity.getToId());
            dto.setRemark(entity.getRemark());
            dto.setAddSource(entity.getAddSource());
            dto.setAddWording(entity.getAddWording());
            // 添加好友
            ResponseVO responseVO = imFriendShipService.doAddFriend(entity.getFromId(), dto, req.getAppId());
            // 如果返回的结果不是添加成功并且也不是对方已经是你的好友
            if(! responseVO.isOk() && responseVO.getCode()!=FriendShipErrorCode.TO_IS_YOUR_FRIEND.getCode()) {
                return responseVO;
            }
        }
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

        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        // 设置好友申请阅读状态为已读
        update.setReadStatus(1);
        // 更新所有查询到的好友申请阅读状态为已读
        imFriendShipRequestMapper.update(update, queryWrapper);
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
