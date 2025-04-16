package com.lld.im.service.conversation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.codec.pack.conversation.DeleteConversationPack;
import com.lld.im.codec.pack.conversation.UpdateConversationPack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ConversationErrorCode;
import com.lld.im.common.enums.ConversationTypeEnum;
import com.lld.im.common.enums.command.ConversationEventCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.SyncReq;
import com.lld.im.common.model.SyncResp;
import com.lld.im.common.model.message.MessageReadContent;
import com.lld.im.service.conversation.dao.ImConversationSetEntity;
import com.lld.im.service.conversation.dao.mapper.ImConversationSetMapper;
import com.lld.im.service.conversation.model.DeleteConversationReq;
import com.lld.im.service.conversation.model.UpdateConversationReq;
import com.lld.im.service.friendship.dao.ImFriendShipEntity;
import com.lld.im.service.seq.RedisSeq;
import com.lld.im.service.utils.MessageProducer;
import com.lld.im.service.utils.WriteUserSeq;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-06
 * @Description:  用来在会话上处理更新我们sequence的类
 * @Version: 1.0
 */

@Service
public class ConversationService {

    @Autowired
    ImConversationSetMapper imConversationSetMapper;


    @Autowired
    MessageProducer messageProducer;


    @Autowired
    AppConfig appConfig;


    @Autowired
    RedisSeq redisSeq;


    @Autowired
    WriteUserSeq writeUserSeq;


    /**
     *  根据传入的信息返回生成的会话id
     * @param type
     * @param fromId
     * @param toId
     * @return
     */
    public String convertConversationId(Integer type,String fromId,String toId) {
        return type + "_" + fromId + "_" + toId;
    }


    /***
     * 把已读的sequence标记到会话上的方法
     * @param messageReadContent
     */
    public void messageMarkRead(MessageReadContent messageReadContent) {
        String toId=messageReadContent.getToId();
        // 如果当前会话种类是群聊的话，则设置toId为群聊Id
        if(messageReadContent.getConversationType()== ConversationTypeEnum.GROUP.getCode()) {
            toId=messageReadContent.getGroupId();
        }
        // 首先根据传入的消息已读类中的信息获得当前会话的id
        String id = convertConversationId(messageReadContent.getConversationType(),
                messageReadContent.getFromId(), toId);
        // 在数据库中查询当前会话
        QueryWrapper<ImConversationSetEntity> query=new QueryWrapper<>();
        query.eq("conversation_id", id);
        query.eq("app_id", messageReadContent.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(query);
        // 如果当前会话在数据库中不存在
        if (imConversationSetEntity == null) {
            // 则需要新建一个数据，存入该会话的sequence并且设置当前会话为已读
            Long seq=redisSeq.doGetSeq(messageReadContent.getAppId()+":"+ Constants.SeqConstants.Conversation);
            imConversationSetEntity=new ImConversationSetEntity();
            imConversationSetEntity.setConversationId(id);
            BeanUtils.copyProperties(messageReadContent,imConversationSetEntity);
            imConversationSetEntity.setToId(toId);
            imConversationSetEntity.setSequence(seq);
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            imConversationSetMapper.insert(imConversationSetEntity);
            writeUserSeq.writeUserSeq(messageReadContent.getAppId(), messageReadContent.getFromId(),Constants.SeqConstants.Conversation,seq);

            // 如果存在的话则更新我们当前会话的read_sequence和sequence值即可
        } else {
            Long seq=redisSeq.doGetSeq(messageReadContent.getAppId()+":"+ Constants.SeqConstants.Conversation);
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            imConversationSetEntity.setSequence(seq);
            imConversationSetMapper.readMark(imConversationSetEntity);
            writeUserSeq.writeUserSeq(messageReadContent.getAppId(), messageReadContent.getFromId(),Constants.SeqConstants.Conversation,seq);
        }

    }


    /***
     * 删除会话
     * @param req
     * @return
     */
    public ResponseVO deleteConversation(DeleteConversationReq req) {

        // 删除会话时是否需要将我们的置顶，免打扰等信息恢复为初始默认值
        // 这里我们默认需要恢复，然后对其进行恢复操作
        QueryWrapper<ImConversationSetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id",req.getConversationId());
        queryWrapper.eq("app_id",req.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(queryWrapper);
        if(imConversationSetEntity != null){
            imConversationSetEntity.setIsMute(0);
            imConversationSetEntity.setIsTop(0);
            imConversationSetMapper.update(imConversationSetEntity,queryWrapper);
        }

        // 如果需要分发给当前用户的其它端删除会话的消息
        if(appConfig.getDeleteConversationSyncMode()==1) {
            DeleteConversationPack pack = new DeleteConversationPack();
            pack.setConversationId(req.getConversationId());
            // 分发给其他在线端
            messageProducer.sendToUserExceptClient(req.getFromId(), ConversationEventCommand.CONVERSATION_DELETE,
                    pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        }
        return ResponseVO.successResponse();

    }


    /***
     *  更新会话(置顶/免打扰)
     *  更新成功后还要向请求用户的其他在线端发送这个消息
     * @param req
     * @return
     */
    public ResponseVO updateConversation(UpdateConversationReq req) {
        // 两个修改参数不能同时为空
        if(req.getIsTop()==null || req.getIsMute()==null) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }
        // 根据id和appId查询数据库中当前会话的记录
        QueryWrapper<ImConversationSetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id",req.getConversationId());
        queryWrapper.eq("app_id",req.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(queryWrapper);
        if(imConversationSetEntity != null){
           Long seq=redisSeq.doGetSeq(req.getAppId()+":"+ Constants.SeqConstants.Conversation);
           if(req.getIsMute()!=null) {
               imConversationSetEntity.setIsMute(req.getIsMute());
           }
           if(req.getIsTop()!=null) {
               imConversationSetEntity.setIsTop(req.getIsTop());
           }
           imConversationSetEntity.setSequence(seq);
           imConversationSetMapper.update(imConversationSetEntity,queryWrapper);
           writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(),Constants.SeqConstants.Conversation,seq);

           // 将修改会话消息发送给请求者的其他在线端
           UpdateConversationPack pack = new UpdateConversationPack();
           pack.setConversationId(req.getConversationId());
           pack.setIsTop(req.getIsTop());
           pack.setIsMute(req.getIsMute());
           pack.setSequence(seq);
           pack.setConversationType(imConversationSetEntity.getConversationType());
           messageProducer.sendToUserExceptClient(req.getFromId(), ConversationEventCommand.CONVERSATION_UPDATE,
                    pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        }
        return ResponseVO.successResponse();
    }


    /***
     * 会话数据增量拉取
     * @param req
     * @return
     */
    public ResponseVO syncConversationSet(SyncReq req) {
        // 首先设置每次最大拉取数量
        if(req.getMaxLimit()>100) {
            req.setMaxLimit(100);
        }
        SyncResp<ImConversationSetEntity> resp=new SyncResp<>();
        // 查询条件：seq > #{seq}  limit #{limit}
        QueryWrapper<ImConversationSetEntity> query=new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("from_id",req.getOperator());
        query.gt("sequence",req.getLastSequence());
        query.last("limit "+req.getMaxLimit());
        query.orderByAsc("sequence");       // 根据friendSequence排序

        List<ImConversationSetEntity> list = imConversationSetMapper.selectList(query);
        if(!CollectionUtils.isEmpty(list)) {
            // 获取我们查询到的最后一个元素
            ImConversationSetEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            // 获取当前用户的最大seq
            resp.setMaxSequence(maxSeqEntity.getSequence());
            Long maxSeq = imConversationSetMapper.getConversationMaxSeq(req.getAppId(), req.getOperator());
            resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
            return ResponseVO.successResponse(resp);

        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }
}
