package com.lld.im.service.friendship.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.lld.im.service.friendship.model.req.FriendDto;
import com.lld.im.service.friendship.model.req.ReadFriendShipRequestReq;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-05
 * @Description: 好友申请模块的业务逻辑接口
 * @Version: 1.0
 */

public interface ImFriendShipRequestService {


    /***
     *  插入一条好友申请
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    public ResponseVO addFriendShipRequest(String fromId, FriendDto dto,Integer appId);


    /***
     * 审批好友申请
     * @param req
     * @return
     */
    public ResponseVO approveFriendRequest(ApproveFriendRequestReq req);


    /***
     * 已读好友申请列表
     * @param req
     * @return
     */
    public ResponseVO readFriendShipRequest(ReadFriendShipRequestReq req);


    /***
     * 获取好友申请列表
     * @param fromId
     * @param appId
     * @return
     */
    public ResponseVO getFriendRequest(String fromId,Integer appId);

}
