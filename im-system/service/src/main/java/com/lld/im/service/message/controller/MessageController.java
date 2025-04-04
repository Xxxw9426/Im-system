package com.lld.im.service.message.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.message.model.req.SendMessageReq;
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
}
