package com.lld.im.service.message.service;

import com.lld.im.codec.pack.message.MessageReadPack;
import com.lld.im.common.enums.command.Command;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.message.MessageReadContent;
import com.lld.im.common.model.message.MessageReceiveAckContent;
import com.lld.im.service.conversation.service.ConversationService;
import com.lld.im.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-03
 * @Description:
 * @Version: 1.0
 */
@Service
public class MessageSyncService {

    @Autowired
    MessageProducer messageProducer;


    @Autowired
    ConversationService conversationService;


    /***
     * 当接收到消息接收方回复的receiveAck后，将其转发给当前客户端的所有在线端，此时的command为1107
     * @param ackContent
     */
    public void receiveMark(MessageReceiveAckContent ackContent ) {
        // 将ack消息发送给接收方的所有在线端
        messageProducer.sendToUser(ackContent.getToId(),
                MessageCommand.MSG_RECEIVE_ACK,ackContent,ackContent.getAppId());
    }


    /***
     * 单聊消息已读的方法：
     *   1. 更新会话的sequence
     *   2. 发送指定command通知在线的同步端
     *   3. 通知消息发送者自己已读(发送已读回执)
     * @param readContent
     */
    public void readMark(MessageReadContent readContent) {
        MessageReadPack readPack = new MessageReadPack();
        BeanUtils.copyProperties(readContent,readPack);
        // 1. 更新会话的sequence
        conversationService.messageMarkRead(readContent);
        // 2. 转发给自己的其他在线端这条消息本用户已读
        syncToSender(readPack,readContent,MessageCommand.MSG_READ_NOTIFY);
        // 3. 发送给消息发送方这条消息自己已读
        messageProducer.sendToUser(readContent.getToId(),MessageCommand.MSG_READ_RECEIPT,
                readPack,readContent.getAppId());
    }


    /***
     * 转发给当前用户的所有其他在线端消息已读的通知
     * @param readContent
     */
    private void syncToSender(MessageReadPack readPack, MessageReadContent readContent, Command command) {
        messageProducer.sendToUserExceptClient(readContent.getFromId(),
                command,readPack,readContent);
    }


    /***
     * 群聊消息已读的方法
     * @param readContent
     */
    public void groupReadMark(MessageReadContent readContent) {
        MessageReadPack readPack = new MessageReadPack();
        BeanUtils.copyProperties(readContent,readPack);
        // 1. 更新会话的sequence
        conversationService.messageMarkRead(readContent);
        // 2. 转发给自己的其他在线端这条消息本用户已读
        syncToSender(readPack,readContent, GroupEventCommand.MSG_GROUP_READ_NOTIFY);
        // 3. 发送给消息发送方这条消息自己已读
        messageProducer.sendToUser(readPack.getToId(),GroupEventCommand.MSG_GROUP_READ_RECEIPT,readContent,readContent.getAppId());
    }
}
