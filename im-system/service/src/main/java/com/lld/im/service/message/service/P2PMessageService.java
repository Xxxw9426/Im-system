package com.lld.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.codec.pack.message.MessageReceiveServerAckPack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ConversationTypeEnum;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.common.model.message.OfflineMessageContent;
import com.lld.im.service.message.model.req.SendMessageReq;
import com.lld.im.service.message.model.resp.SendMessageResp;
import com.lld.im.service.seq.RedisSeq;
import com.lld.im.service.utils.CallbackService;
import com.lld.im.service.utils.ConversationIdGenerate;
import com.lld.im.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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


    @Autowired
    RedisSeq redisSeq;


    @Autowired
    AppConfig appConfig;


    @Autowired
    CallbackService callbackService;


    private final ThreadPoolExecutor threadPoolExecutor;


    {
        AtomicInteger num = new AtomicInteger(0);
        /***
         * 这个方法的参数：
         *   1. 核心线程数
         *   2. 最大线程数
         *   3. 线程存活时间
         *   4. 时间单位
         *   5. 队列列型
         *   6. 线程工厂
         */
        threadPoolExecutor=new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);     // 指定该线程为我们的后台线程
                thread.setName("message-process-thread-" + num.getAndIncrement());    // 给每一个线程设置一个别名
                return thread;
            }
        });
    }


    /***
     * 处理单聊消息逻辑的方法
     * @param messageContent    封装的在业务逻辑层接收传来的单聊消息的类
     */
    public void process(MessageContent messageContent) {

        // 用messageId从缓存中获取消息
        MessageContent cache = messageStoreService.getMessageFromMessageIdCache(messageContent.getAppId(), messageContent.getMessageId(),MessageContent.class);
        // 如果缓存不为空，不需要持久化，直接消息分发即可
        if(cache != null) {
            threadPoolExecutor.execute(()->{
                // 分发消息的主要流程
                // 1. 回ack成功给消息发送者
                ack(messageContent,ResponseVO.successResponse());
                // 2. 发消息给消息发送者同步的在线端
                syncToSender(cache,cache);
                // 3. 发消息给消息接收者同步的在线端
                List<ClientInfo> list = dispatchMessage(cache);
                if(list.isEmpty()) {
                    // 如果接收方不在线，则由服务端发送消息确认ack
                    receiveAck(messageContent);
                }
            });
            return;
        }

        // 回调
        ResponseVO responseVO = ResponseVO.successResponse();
        if(appConfig.isSendMessageAfterCallback()){
            responseVO = callbackService.beforeCallback(messageContent.getAppId(), Constants.CallbackCommand.SendMessageBefore
                    , JSONObject.toJSONString(messageContent));
        }

        if(!responseVO.isOk()){
            ack(messageContent,responseVO);
            return;
        }

        // 将生成seq提取到外面来的目的是，不同服务对于seq的依赖程度不同，我们可以根据我们对seq的依赖程度来实现这部分逻辑，
        // 可以给这段seq有关的代码加上try catch语句，根据我们项目中对seq的需要来完成逻辑。
        // 在插入合法消息之前为这个消息生成一个seq
        // key：appId + Seq + (from + to)
        Long seq = redisSeq.doGetSeq(messageContent.getAppId()+":"+ Constants.SeqConstants.Message+":"+
                ConversationIdGenerate.generateP2PId(messageContent.getFromId(),messageContent.getToId()));
        // 设置到消息中
        messageContent.setMessageSequence(seq);

        // 校验成功，向线程池提交任务
        threadPoolExecutor.execute(()->{
            // 数据持久化，向单聊消息表中插入单聊的消息
            messageStoreService.storeP2PMessage(messageContent);
            // 分发消息的主要流程
            // 1. 回ack成功给消息发送者
            ack(messageContent,ResponseVO.successResponse());
            // 2. 发消息给消息发送者同步的在线端
            syncToSender(messageContent,messageContent);
            // 3. 发消息给消息接收者同步的在线端
            List<ClientInfo> list = dispatchMessage(messageContent);
            // 将messageId存到缓存中
            messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId(),messageContent.getMessageId(),messageContent);
            if(list.isEmpty()) {
                // 如果接收方不在线
                // 向redis中插入离线消息
                OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
                BeanUtils.copyProperties(messageContent,offlineMessageContent);
                offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());
                messageStoreService.storeOfflineMessage(offlineMessageContent);
                // 由服务端发送消息确认ack
                receiveAck(messageContent);
            }

            // 回调
            if(appConfig.isSendMessageAfterCallback()){
                callbackService.callback(messageContent.getAppId(),Constants.CallbackCommand.SendMessageAfter,
                        JSONObject.toJSONString(messageContent));
            }

            logger.info("消息处理完成：{}",messageContent.getMessageId());
        });
    }


    /***
     * 进行前置校验的方法
     * @param fromId   消息发送者
     * @param toId     消息接受者
     * @param appId
     * @return
     */
    public ResponseVO imServerPermissionCheck(String fromId, String toId, Integer appId){
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
                ChatMessageAck(messageContent.getMessageId(),messageContent.getMessageSequence());
        responseVO.setData(chatMessageAck);
        // 发ack消息
        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK,
                responseVO,messageContent
        );
    }


    /***
     * 当消息接收方不在线时由服务端发起的消息确认的方法
     * @param messageContent
     */
    public void receiveAck(MessageContent messageContent) {
        MessageReceiveServerAckPack pack = new MessageReceiveServerAckPack();
        pack.setFromId(messageContent.getToId());
        pack.setToId(messageContent.getFromId());
        pack.setMessageKey(messageContent.getMessageKey());
        pack.setMessageSequence(messageContent.getMessageSequence());
        pack.setServerSend(true);
        // 调用发送给某个特定端的方法
        // ack消息只需要发送发送消息的端，发送消息用户的其它端我们已经进行了转发。
        messageProducer.sendToUser(messageContent.getFromId(),MessageCommand.MSG_RECEIVE_ACK,
                pack,new ClientInfo(messageContent.getAppId(),messageContent.getClientType()
                        ,messageContent.getImei()));
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
     * @return  将转发成功的用户集合返回
     */
    private List<ClientInfo> dispatchMessage(MessageContent messageContent){
        // 直接调用转发的方法
        List<ClientInfo> clientInfos = messageProducer.sendToUser(messageContent.getToId(), MessageCommand.MSG_P2P, messageContent, messageContent.getAppId());
        return clientInfos;
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
