package com.lld.message.service;


import com.lld.im.common.model.message.GroupChatMessageContent;
import com.lld.im.common.model.message.MessageContent;
import com.lld.message.dao.ImGroupMessageHistoryEntity;
import com.lld.message.dao.ImMessageBodyEntity;
import com.lld.message.dao.ImMessageHistoryEntity;
import com.lld.message.dao.mapper.ImGroupMessageHistoryMapper;
import com.lld.message.dao.mapper.ImMessageBodyMapper;
import com.lld.message.dao.mapper.ImMessageHistoryMapper;
import com.lld.message.model.DoStoreGroupMessageDto;
import com.lld.message.model.DoStoreP2PMessageDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-02
 * @Description: 真正处理持久化消息的核心方法类
 * @Version: 1.0
 */
@Service
public class StoreMessageService {

    @Autowired
    ImMessageHistoryMapper imMessageHistoryMapper;


    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;


    @Autowired
    ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;


    /***
     * 向messageBody中插入单聊消息的方法
     * @param doStoreP2PMessageDto
     */
    @Transactional
    public void doStoreP2PMessage(DoStoreP2PMessageDto doStoreP2PMessageDto) {
        // 首先向messageBody表中插入单聊消息
        imMessageBodyMapper.insert(doStoreP2PMessageDto.getImMessageBody());
        // 写扩散
        // 将ImMessageBodyEntity 转化为 messageHistory
        List<ImMessageHistoryEntity> imMessageHistoryEntities = extractToP2PMessageHistory(doStoreP2PMessageDto.getMessageContent(),
                doStoreP2PMessageDto.getImMessageBody());
        // 调用批量插入方法插入数据
        imMessageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
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
        fromHistory.setSequence(messageContent.getMessageSequence());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());
        toHistory.setSequence(messageContent.getMessageSequence());

        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }


    /***
     * 向messageBody中插入群聊消息的方法
     * @param messageDto
     */
    @Transactional
    public void doStoreGroupMessage(DoStoreGroupMessageDto messageDto) {
        // 首先向messageBody表中插入群聊消息
        imMessageBodyMapper.insert(messageDto.getImMessageBodyEntity());
        // 通过groupMessageContent和messageBody转化成groupMessageHistoryEntity，然后调用mapper将其插入groupMessageHistory表中
        ImGroupMessageHistoryEntity imGroupMessageHistoryEntity = extractToGroupMessageHistory(messageDto.getGroupChatMessageContent(), messageDto.getImMessageBodyEntity());
        imGroupMessageHistoryMapper.insert(imGroupMessageHistoryEntity);
    }


    /***
     *  将 GroupMessageContent 转化成 GroupMessageHistory后向 GroupMessageHistory数据库表中插入数据的方法
     * @param content
     * @param imMessageBodyEntity
     * @return
     */
    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent content, ImMessageBodyEntity imMessageBodyEntity){
        ImGroupMessageHistoryEntity result = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(content, result);
        result.setGroupId(content.getGroupId());
        result.setMessageKey(imMessageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());
        return result;
    }
}
