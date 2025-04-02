package com.lld.im.service.message.service;

import com.lld.im.common.enums.DelFlagEnum;
import com.lld.im.common.model.message.GroupChatMessageContent;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.service.group.dao.ImGroupMessageHistoryEntity;
import com.lld.im.service.group.dao.mapper.ImGroupMessageHistoryMapper;
import com.lld.im.service.message.dao.ImMessageBodyEntity;
import com.lld.im.service.message.dao.ImMessageHistoryEntity;
import com.lld.im.service.message.dao.mapper.ImMessageBodyMapper;
import com.lld.im.service.message.dao.mapper.ImMessageHistoryMapper;
import com.lld.im.service.utils.SnowflakeIdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-01
 * @Description: 用来持久化单聊消息数据的类
 * @Version: 1.0
 */
@Service
public class MessageStoreService {


    @Autowired
    ImMessageHistoryMapper imMessageHistoryMapper;


    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;


    @Autowired
    SnowflakeIdWorker snowflakeIdWorker;


    @Autowired
    ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;


    /***
     *  持久化单聊消息的方法
     *  将消息按照 messageBody(以messageKey为键存储消息内容) + messageHistory(存储消息的索引)
     * @param messageContent
     */
    @Transactional
    public void storeP2PMessage(MessageContent messageContent) {
        // 将 MessageContent 转化成 MessageBody
        ImMessageBodyEntity messageBody = extractMessageBody(messageContent);
        // 插入 MessageBody
        imMessageBodyMapper.insert(messageBody);
        // 转化成 MessageHistory
        List<ImMessageHistoryEntity> list = extractToP2PMessageHistory(messageContent, messageBody);
        // 批量插入
        messageContent.setMessageKey(messageBody.getMessageKey());
        imMessageHistoryMapper.insertBatchSomeColumn(list);
    }


    /***
     *  将 MessageContent 转化成 MessageBody 的方法
     * @param messageContent
     * @return
     */
    public ImMessageBodyEntity extractMessageBody(MessageContent messageContent) {
        ImMessageBodyEntity messageBody = new ImMessageBodyEntity();
        messageBody.setAppId(messageContent.getAppId());
        messageBody.setMessageKey(snowflakeIdWorker.nextId());
        messageBody.setCreateTime(System.currentTimeMillis());
        messageBody.setSecurityKey("");      // 预留字段
        messageBody.setExtra(messageContent.getExtra());            // 拓展字段
        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBody.setMessageTime(messageContent.getMessageTime());      // 客户端发送消息时间
        messageBody.setMessageBody(messageContent.getMessageBody());
        return messageBody;
    }


    /***
     * 将 messageBody 转化成 messageHistory后向 messageHistory数据库表中插入数据的方法
     * @param messageContent
     * @param imMessageBodyEntity
     * @return
     */
    public List<ImMessageHistoryEntity> extractToP2PMessageHistory(MessageContent messageContent,
                                                                   ImMessageBodyEntity imMessageBodyEntity){
        List<ImMessageHistoryEntity> list=new ArrayList<>();
        ImMessageHistoryEntity fromHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());

        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }


    /***
     *  持久化群聊消息的方法
     *  将消息按照 messageBody(以messageKey为键存储消息内容) + messageHistory(存储消息的索引)
     * @param groupChatMessageContent
     */
    @Transactional
    public void storeGroupMessage(GroupChatMessageContent groupChatMessageContent) {
        // 将 GroupMessageContent 转化成 MessageBody
        ImMessageBodyEntity messageBody = extractMessageBody(groupChatMessageContent);
        // 插入 MessageBody
        imMessageBodyMapper.insert(messageBody);
        // 转化成 GroupMessageHistory
        ImGroupMessageHistoryEntity entity = extractToGroupMessageHistory(groupChatMessageContent, messageBody);
        imGroupMessageHistoryMapper.insert(entity);
        groupChatMessageContent.setMessageKey(messageBody.getMessageKey());
    }


    /***
     *  将 GroupMessageBody 转化成 GroupMessageHistory后向 GroupMessageHistory后向数据库表中插入数据的方法
     * @param content
     * @param imMessageBodyEntity
     * @return
     */
    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent content,ImMessageBodyEntity imMessageBodyEntity){
        ImGroupMessageHistoryEntity result = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(content, result);
        result.setGroupId(content.getGroupId());
        result.setMessageKey(imMessageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());
        return result;
    }
}
