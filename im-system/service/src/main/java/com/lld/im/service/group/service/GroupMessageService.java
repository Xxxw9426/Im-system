package com.lld.im.service.group.service;

import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.message.GroupChatMessageContent;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.service.group.model.req.SendGroupMessageReq;
import com.lld.im.service.message.model.resp.SendMessageResp;
import com.lld.im.service.message.service.CheckSendMessageService;
import com.lld.im.service.message.service.MessageStoreService;
import com.lld.im.service.message.service.P2PMessageService;
import com.lld.im.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-01
 * @Description: 处理群聊消息的业务逻辑类
 * @Version: 1.0
 */

@Service
public class GroupMessageService {

    @Autowired
    CheckSendMessageService checkSendMessageService;


    @Autowired
    MessageProducer messageProducer;


    @Autowired
    ImGroupMemberService imGroupMemberService;


    @Autowired
    MessageStoreService messageStoreService;


    /***
     * 处理群聊消息逻辑的方法
     * @param groupContent   封装的在业务逻辑层接收传来的群聊消息的类
     */
    public void process(GroupChatMessageContent groupContent) {
        String fromId = groupContent.getFromId();
        String groupId = groupContent.getGroupId();
        Integer appId = groupContent.getAppId();
        // 首先进行前置校验
        ResponseVO responseVO = imServerPermissionCheck(fromId, groupId, appId);
        if(responseVO.isOk()) {
            // 将数据持久化到数据库
            messageStoreService.storeGroupMessage(groupContent);
            // 分发消息的主要流程
            // 1. 回ack成功给消息发送者
            ack(groupContent,responseVO);
            // 2. 发消息给消息发送者同步的在线端
            syncToSender(groupContent,groupContent);
            // 3. 发消息给所有群成员同步的在线端
            dispatchMessage(groupContent);

        } else {
            // 告诉客户端失败了
            // 回ack失败
            ack(groupContent,responseVO);
        }
    }


    /***
     * 进行前置校验的方法
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    private ResponseVO imServerPermissionCheck(String fromId,String groupId, Integer appId) {
        ResponseVO responseVO = checkSendMessageService
                .checkGroupMessage(fromId,groupId,appId);
        return responseVO;
    }


    /***
     * 返回ack的方法
     * @param groupContent
     * @param responseVO
     */
    private void ack(GroupChatMessageContent groupContent,ResponseVO responseVO) {
        ChatMessageAck chatMessageAck = new
                ChatMessageAck(groupContent.getMessageId());
        responseVO.setData(chatMessageAck);
        // 发ack消息
        messageProducer.sendToUser(groupContent.getFromId(), GroupEventCommand.MSG_GROUP,
                responseVO,groupContent);
    }


    /***
     * 发消息给消息发送者同步的在线端(数据同步)
     * @param groupContent    封装的在业务逻辑层接收传来的单聊消息的类
     * @param clientInfo        消息要发送给的客户端的端信息
     */
    private void syncToSender(GroupChatMessageContent groupContent, ClientInfo clientInfo) {
        // 直接调用转发的方法
        messageProducer.sendToUserExceptClient(groupContent.getFromId(),GroupEventCommand.MSG_GROUP,
                groupContent,clientInfo);
    }


    /***
     *  转发消息给群聊中所有群成员的的所有在线端
     * @param groupContent
     */
    private void dispatchMessage(GroupChatMessageContent groupContent) {
        List<String> groupMemberIds = imGroupMemberService.getGroupMemberId(groupContent.getGroupId(),
                groupContent.getAppId());
        for(String groupMemberId : groupMemberIds) {
            // 排除当前用户是发送消息的用户
            if(!groupMemberId.equals(groupContent.getFromId())) {
                messageProducer.sendToUser(groupMemberId,GroupEventCommand.MSG_GROUP,groupContent,groupContent.getAppId());
            }
        }
    }


    /***
     * 提供的接入IM服务的服务或者APP管理员的单聊发消息的方法
     * @param req
     * @return
     */
    public SendMessageResp send(SendGroupMessageReq req) {
        SendMessageResp resp = new SendMessageResp();
        GroupChatMessageContent content = new GroupChatMessageContent();
        BeanUtils.copyProperties(req,content);
        // 向数据库中插入数据
        messageStoreService.storeGroupMessage(content);
        resp.setMessageKey(content.getMessageKey());
        resp.setMessageTime(System.currentTimeMillis());
        // 发消息给消息发送者同步的在线端
        syncToSender(content,content);
        // 发消息给所有群成员同步的在线端
        dispatchMessage(content);
        return resp;
    }
}
