package com.lld.im.service.conversation.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.model.SyncReq;
import com.lld.im.service.conversation.model.DeleteConversationReq;
import com.lld.im.service.conversation.model.UpdateConversationReq;
import com.lld.im.service.conversation.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-07
 * @Description: 会话模块的controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("v1/conversation")
public class ConversationController {

    @Autowired
    ConversationService conversationService;


    /***
     * 删除会话
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/deleteConversation")
    public ResponseVO deleteConversation(@RequestBody @Validated DeleteConversationReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return conversationService.deleteConversation(req);
    }


    /***
     * 修改会话
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/updateConversation")
    public ResponseVO updateConversation(@RequestBody @Validated UpdateConversationReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return conversationService.updateConversation(req);
    }


    /***
     * 会话数据增量拉取
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/syncConversationList")
    public ResponseVO syncFriendShipList(@RequestBody @Validated SyncReq req, Integer appId)  {
        req.setAppId(appId);
        return conversationService.syncConversationSet(req);
    }
}
