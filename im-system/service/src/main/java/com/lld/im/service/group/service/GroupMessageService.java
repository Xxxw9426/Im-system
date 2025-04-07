package com.lld.im.service.group.service;

import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.message.GroupChatMessageContent;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.common.model.message.OfflineMessageContent;
import com.lld.im.service.group.model.req.SendGroupMessageReq;
import com.lld.im.service.message.model.resp.SendMessageResp;
import com.lld.im.service.message.service.CheckSendMessageService;
import com.lld.im.service.message.service.MessageStoreService;
import com.lld.im.service.message.service.P2PMessageService;
import com.lld.im.service.seq.RedisSeq;
import com.lld.im.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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


    @Autowired
    RedisSeq redisSeq;


    private final ThreadPoolExecutor threadPoolExecutor;


    {
        AtomicInteger num = new AtomicInteger(0);
        threadPoolExecutor=new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);     // 指定该线程为我们的后台线程
                thread.setName("message-group-thread-" + num.getAndIncrement());    // 给每一个线程设置一个别名
                return thread;
            }
        });
    }


    /***
     * 处理群聊消息逻辑的方法
     * @param groupContent   封装的在业务逻辑层接收传来的群聊消息的类
     */
    public void process(GroupChatMessageContent groupContent) {
        // 首先判断缓存中是否已经有该消息
        GroupChatMessageContent cache = messageStoreService.getMessageFromMessageIdCache(groupContent.getAppId(), groupContent.getMessageId(),
                GroupChatMessageContent.class);
        // 如果已经缓存中已经有了这个消息的话，只需要分发，不需要写入缓存
        if(cache != null) {
            threadPoolExecutor.execute(()->{
                // 分发消息的主要流程
                // 1. 回ack成功给消息发送者
                ack(groupContent,ResponseVO.successResponse());
                // 2. 发消息给消息发送者同步的在线端
                syncToSender(cache,cache);
                // 3. 发消息给所有群成员同步的在线端
                dispatchMessage(cache);
            });
        }

        // 给我们的群聊消息加上我们的序列号
        Long seq = redisSeq.doGetSeq(groupContent.getAppId() + ":" + Constants.SeqConstants.GroupMessage + groupContent.getGroupId());
        groupContent.setMessageSequence(seq);

        threadPoolExecutor.execute(()->{
            // 将数据持久化到数据库
            messageStoreService.storeGroupMessage(groupContent);
            // 插入离线群聊消息
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(groupContent, offlineMessageContent);
            offlineMessageContent.setToId(groupContent.getGroupId());
            List<String> groupMemberIds = imGroupMemberService.getGroupMemberId(groupContent.getGroupId(),
                    groupContent.getAppId());
            groupContent.setMemberId(groupMemberIds);
            messageStoreService.storeGroupOfflineMessage(offlineMessageContent,groupMemberIds);
            // 分发消息的主要流程
            // 1. 回ack成功给消息发送者
            ack(groupContent,ResponseVO.successResponse());
            // 2. 发消息给消息发送者同步的在线端
            syncToSender(groupContent,groupContent);
            // 3. 发消息给所有群成员同步的在线端
            dispatchMessage(groupContent);
            // 将当前消息加入缓存
            messageStoreService.setMessageFromMessageIdCache(groupContent.getAppId(),groupContent.getMessageId(),groupContent);
        });
    }


    /***
     * 进行前置校验的方法
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO imServerPermissionCheck(String fromId,String groupId, Integer appId) {
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
        chatMessageAck.setMessageSequence(groupContent.getMessageSequence());
        responseVO.setData(chatMessageAck);
        // 发ack消息
        messageProducer.sendToUser(groupContent.getFromId(), GroupEventCommand.GROUP_MSG_ACK,
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
        for(String groupMemberId : groupContent.getMemberId()) {
            // 排除当前用户是发送消息的用户
            if(!groupMemberId.equals(groupContent.getFromId())) {
                messageProducer.sendToUser(groupMemberId,GroupEventCommand.MSG_GROUP,groupContent,groupContent.getAppId());
            }
        }
    }


    /***
     * 提供的接入IM服务的服务或者APP管理员的群聊发消息的方法
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
