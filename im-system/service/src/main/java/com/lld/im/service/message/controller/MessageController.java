package com.lld.im.service.message.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.SyncReq;
import com.lld.im.common.model.message.CheckSendMessageReq;
import com.lld.im.service.group.service.GroupMessageService;
import com.lld.im.service.message.model.req.SendMessageReq;
import com.lld.im.service.message.service.MessageSyncService;
import com.lld.im.service.message.service.P2PMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-02
 * @Description: 面向接入IM服务的系统和APP管理员的接口层
 * @Version: 1.0
 */
@RestController
@RequestMapping("v1/message")
public class MessageController {

    @Autowired
    P2PMessageService p2PMessageService;


    @Autowired
    GroupMessageService groupMessageService;


    @Autowired
    MessageSyncService messageSyncService;


    /***
     * 提供的接入IM服务的服务或者APP管理员的单聊发消息的接口
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/send")
    public ResponseVO send(@RequestBody @Validated SendMessageReq req,Integer appId) {
        req.setAppId(appId);
        return ResponseVO.successResponse(p2PMessageService.send(req));
    }


    /***
     * 系统内部rpc调用的接口，对消息进行前置校验
     * @param req
     * @return
     */
    @RequestMapping("/checkSend")
    public ResponseVO checkSend(@RequestBody @Validated CheckSendMessageReq req) {
        if(req.getCommand()== MessageCommand.MSG_P2P.getCommand()) {
            return p2PMessageService.imServerPermissionCheck(req.getFromId(), req.getToId(), req.getAppId());
        } else {
            return groupMessageService.imServerPermissionCheck(req.getFromId(),req.getToId(),req.getAppId());
        }
    }


    /***
     * 用户上线后主动拉取离线消息的接口
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/syncOfflineMessage")
    public ResponseVO syncOfflineMessage(@RequestBody @Validated SyncReq req, Integer appId)  {
        req.setAppId(appId);
        return messageSyncService.syncOfflineMessage(req);
    }
}
