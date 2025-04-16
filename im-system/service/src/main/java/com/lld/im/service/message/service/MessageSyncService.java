package com.lld.im.service.message.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.codec.pack.message.MessageReadPack;
import com.lld.im.codec.pack.message.RecallMessageNotifyPack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ConversationTypeEnum;
import com.lld.im.common.enums.DelFlagEnum;
import com.lld.im.common.enums.MessageErrorCode;
import com.lld.im.common.enums.command.Command;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.SyncReq;
import com.lld.im.common.model.SyncResp;
import com.lld.im.common.model.message.MessageReadContent;
import com.lld.im.common.model.message.MessageReceiveAckContent;
import com.lld.im.common.model.message.OfflineMessageContent;
import com.lld.im.common.model.message.RecallMessageContent;
import com.lld.im.service.conversation.service.ConversationService;
import com.lld.im.service.group.service.ImGroupMemberService;
import com.lld.im.service.message.dao.ImMessageBodyEntity;
import com.lld.im.service.message.dao.mapper.ImMessageBodyMapper;
import com.lld.im.service.seq.RedisSeq;
import com.lld.im.service.utils.ConversationIdGenerate;
import com.lld.im.service.utils.GroupMessageProducer;
import com.lld.im.service.utils.MessageProducer;
import com.lld.im.service.utils.SnowflakeIdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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


    @Autowired
    RedisTemplate redisTemplate;


    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;


    @Autowired
    RedisSeq redisSeq;


    @Autowired
    SnowflakeIdWorker snowflakeIdWorker;


    @Autowired
    ImGroupMemberService imGroupMemberService;


    @Autowired
    GroupMessageProducer groupMessageProducer;


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


    /***
     * 用户上线后主动拉取离线消息的方法
     * @param req
     * @return
     */
    public ResponseVO syncOfflineMessage(SyncReq req) {
        SyncResp<OfflineMessageContent> resp=new SyncResp<>();
        String offlineMessageKey=req.getAppId()+":"+ Constants.RedisConstants.OfflineMessage+":"+req.getOperator();
        // 首先查找离线消息最大的seq
        Long maxSeq=0L;
        ZSetOperations operations = redisTemplate.opsForZSet();
        Set set = operations.reverseRangeWithScores(offlineMessageKey, 0, 0);
        // 如果查找的集合不为空
        if(!CollectionUtil.isEmpty(set)) {
            List list=new ArrayList(set);
            DefaultTypedTuple o= (DefaultTypedTuple) list.get(0);
            maxSeq=o.getScore().longValue();
        }
        // 要返回的离线消息集合
        List<OfflineMessageContent> respList=new ArrayList<>();
        resp.setMaxSequence(maxSeq);
        // 查找离线消息
        // 查找上一次的seq到此次的maxSeq之间的所有数据
        Set<ZSetOperations.TypedTuple> querySet = operations.rangeByScoreWithScores(offlineMessageKey,
                req.getLastSequence(), maxSeq, 0, req.getMaxLimit());
        // 遍历所有数据
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            String value = typedTuple.getValue();
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            respList.add(offlineMessageContent);
        }
        // 将结果集加入结果
        resp.setDataList(respList);
        if(!CollectionUtil.isEmpty(respList)) {
            // 获得结果集中seq的最大值，与我们查找到的最大值进行比较，判断是否拉取结束
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(maxSeq<=offlineMessageContent.getMessageKey());
        }
        return ResponseVO.successResponse(resp);
    }


    /***
     *  处理撤回消息的方法
     *   1. 修改历史消息的状态
     *   2. 修改离线消息的状态
     *   3. 回包ack给消息的发送方
     *   4. 发送方同步给自己的其他在线端
     *   5. 分发消息给消息接收方的所有在线端
     * @param content
     */
    public void recallMessage(RecallMessageContent content) {

        // 1. 修改历史消息的状态

        // 获取消息的发送时间
        long messageTime = content.getMessageTime();
        // 获取当前时间
        long now = System.currentTimeMillis();
        // 返回类
        RecallMessageNotifyPack pack = new RecallMessageNotifyPack();
        BeanUtils.copyProperties(content,pack);
        // 判断消息撤回时间是否超出限制
        // 我们定义撤回时间为两分钟
        if( now-messageTime > 120000L ) {
            // ack返回失败：超过两分钟的消息不能撤回
            recallAck(pack,ResponseVO.errorResponse(MessageErrorCode.MESSAGE_RECALL_TIME_OUT),content);
            return;
        }
        // 查找到要撤回的消息
        QueryWrapper<ImMessageBodyEntity> query=new QueryWrapper<>();
        query.eq("app_id",content.getAppId());
        query.eq("message_key",content.getMessageKey());
        ImMessageBodyEntity body = imMessageBodyMapper.selectOne(query);
        // 消息不存在
        if(body == null){
            // ack返回失败：不存在的消息不能撤回
            recallAck(pack,ResponseVO.errorResponse(MessageErrorCode.MESSAGEBODY_IS_NOT_EXIST),content);
            return;
        }
        // 消息已删除
        if(body.getDelFlag() == DelFlagEnum.DELETE.getCode()){
            // ack返回失败：已经撤回的消息不能撤回
            recallAck(pack,ResponseVO.errorResponse(MessageErrorCode.MESSAGE_IS_RECALLED),content);
            return;
        }

        // 2. 修改离线消息的状态
        // 判断这个离线消息是群聊的离线消息还是私聊的离线消息：私聊的离线消息我们需要修改双方的消息记录，而群聊只需要修改一个
        // 是单聊消息
        if(content.getConversationType()== ConversationTypeEnum.P2P.getCode()) {
            // 找到fromId的队列
            String fromKey = content.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + content.getFromId();
            // 找到toId的队列
            String toKey = content.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + content.getToId(); OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(content,offlineMessageContent);
            offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
            offlineMessageContent.setMessageKey(content.getMessageKey());
            offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());
            offlineMessageContent.setConversationId(conversationService.convertConversationId(offlineMessageContent.getConversationType()
                    ,content.getFromId(),content.getToId()));
            offlineMessageContent.setMessageBody(body.getMessageBody());
            long seq = redisSeq.doGetSeq(content.getAppId() + ":" + Constants.SeqConstants.Message + ":" + ConversationIdGenerate.generateP2PId(content.getFromId(),content.getToId()));
            offlineMessageContent.setMessageSequence(seq);
            long messageKey = SnowflakeIdWorker.nextId();
            redisTemplate.opsForZSet().add(fromKey,JSONObject.toJSONString(offlineMessageContent),messageKey);
            redisTemplate.opsForZSet().add(toKey,JSONObject.toJSONString(offlineMessageContent),messageKey);

            // 响应ack
            recallAck(pack,ResponseVO.successResponse(),content);
            //分发给同步端
            messageProducer.sendToUserExceptClient(content.getFromId(),
                    MessageCommand.MSG_RECALL_NOTIFY,pack,content);
            //分发给对方
            messageProducer.sendToUser(content.getToId(),MessageCommand.MSG_RECALL_NOTIFY,
                    pack,content.getAppId());

        // 是群聊消息
        } else {
            List<String> groupMemberId = imGroupMemberService.getGroupMemberId(content.getToId(), content.getAppId());
            long seq = redisSeq.doGetSeq(content.getAppId() + ":" + Constants.SeqConstants.Message + ":" + ConversationIdGenerate.generateP2PId(content.getFromId(),content.getToId()));
            // ack
            recallAck(pack,ResponseVO.successResponse(),content);
            // 发送给同步端
            messageProducer.sendToUserExceptClient(content.getFromId(), MessageCommand.MSG_RECALL_NOTIFY, pack
                    , content);
            for (String memberId : groupMemberId) {
                String toKey = content.getAppId() + ":" + Constants.SeqConstants.Message + ":" + memberId;
                OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
                offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
                BeanUtils.copyProperties(content,offlineMessageContent);
                offlineMessageContent.setConversationType(ConversationTypeEnum.GROUP.getCode());
                offlineMessageContent.setConversationId(conversationService.convertConversationId(offlineMessageContent.getConversationType()
                        ,content.getFromId(),content.getToId()));
                offlineMessageContent.setMessageBody(body.getMessageBody());
                offlineMessageContent.setMessageSequence(seq);
                redisTemplate.opsForZSet().add(toKey,JSONObject.toJSONString(offlineMessageContent),seq);
                groupMessageProducer.producer(content.getFromId(), MessageCommand.MSG_RECALL_NOTIFY, pack,content);
            }
        }
    }


    /***
     * 向客户端响应ack的方法
     * @param recallPack
     * @param success
     * @param clientInfo
     */
    private void recallAck(RecallMessageNotifyPack recallPack, ResponseVO<Object> success, ClientInfo clientInfo) {
        ResponseVO<Object> wrappedResp = success;
        messageProducer.sendToUser(recallPack.getFromId(),
                MessageCommand.MSG_RECALL_ACK, wrappedResp, clientInfo);
    }
}
