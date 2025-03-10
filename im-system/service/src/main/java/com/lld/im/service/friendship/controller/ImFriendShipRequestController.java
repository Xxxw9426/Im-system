package com.lld.im.service.friendship.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.lld.im.service.friendship.model.req.GetFriendShipRequestReq;
import com.lld.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.lld.im.service.friendship.service.ImFriendShipRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-05
 * @Description: 好友申请模块controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("v1/friendship/request")
public class ImFriendShipRequestController {

    @Autowired
    ImFriendShipRequestService imFriendShipRequestService;


    /***
     *  审批好友申请
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/approveFriendRequest")
    public ResponseVO approveFriendRequest(@RequestBody @Validated ApproveFriendRequestReq req,Integer appId,String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imFriendShipRequestService.approveFriendRequest(req);
    }


    /***
     * 已读好友申请列表
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/readFriendShipRequest")
    public ResponseVO readFriendShipRequest(@RequestBody @Validated ReadFriendShipRequestReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipRequestService.readFriendShipRequest(req);
    }


    /***
     * 获取好友申请列表
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/getFriendRequest")
    public ResponseVO getFriendRequest(@RequestBody @Validated GetFriendShipRequestReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipRequestService.getFriendRequest(req.getFromId(),appId);
    }
}
