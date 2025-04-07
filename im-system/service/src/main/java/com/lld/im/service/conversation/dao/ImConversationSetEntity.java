package com.lld.im.service.conversation.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
@TableName("im_conversation_set")
public class ImConversationSetEntity {

    //会话id 0_fromId_toId (会话类型+fromId+toId)
    private String conversationId;

    //会话类型  0:单聊  1:群聊
    private Integer conversationType;

    private String fromId;

    private String toId;

    private int isMute;     // 是否免打扰

    private int isTop;

    private Long sequence;         // 当前会话的序列号

    private Long readSequence;    // 记录已读到的消息的序列号

    private Integer appId;
}
