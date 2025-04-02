package com.lld.im.service.message.service;

import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.service.message.model.req.SendMessageReq;
import com.lld.im.service.message.model.resp.SendMessageResp;
import com.lld.im.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-01
 * @Description: 单聊消息处理的业务逻辑层
 * @Version: 1.0
 */
@Service
public class P2PMessageService {

    private static Logger logger = LoggerFactory.getLogger(P2PMessageService.class);


    @Autowired
    CheckSendMessageService checkSendMessageService;


    @Autowired
    MessageProducer messageProducer;


    @Autowired
    MessageStoreService messageStoreService;


    /***
     * 处理单聊消息逻辑的方法
     * @param messageContent    封装的在业务逻辑层接收传来的单聊消息的类
     */
    public void process(MessageContent messageContent) {
        String fromId = messageContent.getFromId();
        String toId = messageContent.getToId();
        Integer appId = messageContent.getAppId();
        // 首先进行前置校验
        // 校验这个用户是否被禁言，是否被禁用
        // 校验发送方和接收方是否是好友(不绝对，取决于业务系统的设计与需求)
        ResponseVO responseVO = imServerPermissionCheck(fromId, toId, appId);
        if(responseVO.isOk()) {
            // 数据持久化，向单聊消息表中插入单聊的消息
            messageStoreService.storeP2PMessage(messageContent);
            // 分发消息的主要流程
            // 1. 回ack成功给消息发送者
            ack(messageContent,responseVO);
            // 2. 发消息给消息发送者同步的在线端
            syncToSender(messageContent,messageContent);
            // 3. 发消息给消息接收者同步的在线端
            dispatchMessage(messageContent);
        } else {
            // 告诉客户端失败了
            // 回ack失败
            ack(messageContent,responseVO);
        }
    }


    /***
     * 进行前置校验的方法
     * @param fromId   消息发送者
     * @param toId     消息接受者
     * @param appId
     * @return
     */
    private ResponseVO imServerPermissionCheck(String fromId, String toId, Integer appId){
        // 校验发送消息的用户是否被禁用或者禁言
        ResponseVO responseVO = checkSendMessageService.checkSenderForbidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }
        // 检验发送消息和接收消息的用户之间是否有好友关系
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }


    /***
     * 返回ack的方法
     * @param messageContent    封装的在业务逻辑层接收传来的单聊消息的类
     * @param responseVO        请求传入业务逻辑层后的前置校验结果信息
     */
    private void ack(MessageContent messageContent, ResponseVO responseVO) {

        logger.info("msg ack,msgId={},checkResult{}",messageContent.getMessageId(),responseVO.getCode());
        ChatMessageAck chatMessageAck = new
                ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        // 发ack消息
        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK,
                responseVO,messageContent
        );
    }


    /***
     * 发消息给消息发送者同步的在线端(数据同步)
     * @param messageContent    封装的在业务逻辑层接收传来的单聊消息的类
     * @param clientInfo        消息要发送给的客户端的端信息
     */
    private void syncToSender(MessageContent messageContent, ClientInfo clientInfo) {
        // 直接调用转发的方法
        messageProducer.sendToUserExceptClient(messageContent.getFromId(),MessageCommand.MSG_P2P,messageContent,clientInfo);
    }


    /***
     * 发送给消息接收者的所有在线端
     * @param messageContent  封装的在业务逻辑层接收传来的单聊消息的类
     * @return
     */
    private void dispatchMessage(MessageContent messageContent){
        // 直接调用转发的方法
        messageProducer.sendToUser(messageContent.getToId(),MessageCommand.MSG_P2P,messageContent,messageContent.getAppId());
    }


    /***
     * 提供的接入IM服务的服务或者APP管理员的单聊发消息的方法
     * @param req
     * @return
     */
    public SendMessageResp send(SendMessageReq req) {
        SendMessageResp messageResp = new SendMessageResp();
        MessageContent messageContent = new MessageContent();
        BeanUtils.copyProperties(req, messageContent);
        // 在数据库中插入数据进行持久化
        messageStoreService.storeP2PMessage(messageContent);
        messageResp.setMessageKey(messageContent.getMessageKey());
        messageResp.setMessageTime(System.currentTimeMillis());

        //2.发消息给同步在线端
        syncToSender(messageContent,messageContent);
        //3.发消息给对方在线端
        dispatchMessage(messageContent);
        return messageResp;
    }
}
