package com.lld.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ConversationTypeEnum;
import com.lld.im.common.enums.DelFlagEnum;
import com.lld.im.common.model.DoStoreGroupMessageDto;
import com.lld.im.common.model.message.*;
import com.lld.im.service.conversation.service.ConversationService;
import com.lld.im.service.group.dao.ImGroupMessageHistoryEntity;
import com.lld.im.service.group.dao.mapper.ImGroupMessageHistoryMapper;
import com.lld.im.service.message.dao.ImMessageBodyEntity;
import com.lld.im.service.message.dao.ImMessageHistoryEntity;
import com.lld.im.service.message.dao.mapper.ImMessageBodyMapper;
import com.lld.im.service.message.dao.mapper.ImMessageHistoryMapper;
import com.lld.im.service.utils.SnowflakeIdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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


    @Autowired
    RabbitTemplate rabbitTemplate;


    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @Autowired
    ConversationService conversationService;


    @Autowired
    AppConfig appConfig;


    /***
     *  持久化单聊消息的方法
     *  将消息按照 messageBody(以messageKey为键存储消息内容) + messageHistory(存储消息的索引)
     * @param messageContent
     */
    @Transactional
    public void storeP2PMessage(MessageContent messageContent) {
        // 将 MessageContent 转化成 MessageBody
        // 并且将其存入向MQ中发送消息的实体类中
        ImMessageBody messageBody = extractMessageBody(messageContent);
        DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
        dto.setMessageContent(messageContent);
        dto.setImMessageBody(messageBody);
        messageContent.setMessageKey(messageBody.getMessageKey());
        // 向MQ发送消息，让MQ异步处理数据持久化
        /***
         *  该方法的参数：
         *    1. 交换机
         *    2. routing-key
         *    3. 发送给MQ的消息内容对象
         */
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreP2PMessage,"", JSONObject.toJSONString(dto));
    }


    /***
     *  将 MessageContent 转化成 MessageBody 的方法
     * @param messageContent
     * @return
     */
    public ImMessageBody extractMessageBody(MessageContent messageContent) {
        ImMessageBody messageBody = new ImMessageBody();
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
        // 将 GroupMessageContent 转化成传入message-store服务的类进行持久化
        ImMessageBody imMessageBody = extractMessageBody(groupChatMessageContent);
        DoStoreGroupMessageDto doStoreGroupMessageDto = new DoStoreGroupMessageDto();
        doStoreGroupMessageDto.setMessageBody(imMessageBody);
        doStoreGroupMessageDto.setGroupChatMessageContent(groupChatMessageContent);
        // 向MQ发送消息，让MQ异步处理数据持久化
        /***
         *  该方法的参数：
         *    1. 交换机
         *    2. routing-key
         *    3. 发送给MQ的消息内容对象
         */
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreGroupMessage,"", JSONObject.toJSONString(doStoreGroupMessageDto));
        groupChatMessageContent.setMessageKey(imMessageBody.getMessageKey());
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


    /***
     * 将单聊和群聊的消息的messageId存入我们的缓存当中
     * @param messageContent
     */
    public void setMessageFromMessageIdCache(Integer appId,String messageId,Object messageContent) {
        // key:appId+cache+messageId
        String key=appId+":"+Constants.RedisConstants.cacheMessage+":"+messageId;
        stringRedisTemplate.opsForValue().set(key,JSONObject.toJSONString(messageContent),300, TimeUnit.SECONDS);
    }


    /***
     * 根据messageId从内存中获取信息
     * @param appId
     * @param messageId
     * @return
     */
    public <T> T getMessageFromMessageIdCache(Integer appId,String messageId,Class<T> clazz) {
        // key:appId+cache+messageId
        String key=appId+":"+Constants.RedisConstants.cacheMessage+":"+messageId;
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isBlank(s)){
            return null;
        }
        return JSONObject.parseObject(s,clazz);
    }


    /***
     * 存储单人离线消息(采用写扩散模式，因此同一个消息要同时写入消息发送方的redis中和消息接收方的redis中)
     * 1. 首先判断fromId用户的队列中数据是否超过限定值，然后向fromId用户的队列中插入数据 根据messageKey作为分值
     * 2. 然后判断toId用户的队列中数据是否超过限定值，接着向toId用户的队列中插入数据 根据messageKey作为分值
     * @param offlineMessage
     */
    public void storeOfflineMessage(OfflineMessageContent offlineMessage) {
        // 离线消息Redis存储的key值
        String fromKey=offlineMessage.getAppId()+":"+Constants.RedisConstants.OfflineMessage+":"+offlineMessage.getFromId();
        String toKey=offlineMessage.getAppId()+":"+Constants.RedisConstants.OfflineMessage+":"+offlineMessage.getToId();
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        // 如果消息发送者的队列中的数据大于配置值
        if(operations.zCard(fromKey)>appConfig.getOfflineMessageCount()) {
            // 则删除掉第一条数据
            operations.removeRange(fromKey,0,0);
        }
        // 给离线消息设置conversationId
        offlineMessage.setConversationId(conversationService.convertConversationId(ConversationTypeEnum.P2P.getCode(),
                offlineMessage.getFromId(), offlineMessage.getToId()));
        // 向Redis中插入离线聊天数据
        /***
         *  这个方法的参数：
         *  1. redis中存储的消息的键值
         *  2. 存储的内容，即我们的消息体
         *  3. 分值：也就是我们区分每条消息的值
         */
        operations.add(fromKey,JSONObject.toJSONString(offlineMessage),offlineMessage.getMessageKey());

        // 如果消息接收者的队列中的数据大于配置值
        if(operations.zCard(toKey)>appConfig.getOfflineMessageCount()) {
            // 则删除掉第一条数据
            operations.removeRange(toKey,0,0);
        }
        // 给离线消息设置conversationId
        offlineMessage.setConversationId(conversationService.convertConversationId(ConversationTypeEnum.P2P.getCode(),
                offlineMessage.getToId(), offlineMessage.getFromId()));
        // 向Redis中插入离线聊天数据
        operations.add(toKey,JSONObject.toJSONString(offlineMessage),offlineMessage.getMessageKey());
    }


    /***
     * 存储群聊的离线消息
     * @param offlineMessage
     * @param memberId
     */
    public void storeGroupOfflineMessage(OfflineMessageContent offlineMessage,List<String> memberId){
        offlineMessage.setConversationType(ConversationTypeEnum.GROUP.getCode());
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        // 向群聊中的所有成员的Redis中插入离线聊天数据
        for(String id:memberId) {
            String toKey=offlineMessage.getAppId()+":"+Constants.RedisConstants.OfflineMessage+":"+id;
            // 给离线消息设置conversationId
            offlineMessage.setConversationId(conversationService.convertConversationId(ConversationTypeEnum.GROUP.getCode(),
                    id, offlineMessage.getToId()));
            // 如果消息发送者的队列中的数据大于配置值
            if(operations.zCard(toKey)>appConfig.getOfflineMessageCount()) {
                // 则删除掉第一条数据
                operations.removeRange(toKey,0,0);
            }
            operations.add(toKey,JSONObject.toJSONString(offlineMessage),offlineMessage.getMessageKey());
        }
    }
}
