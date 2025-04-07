package com.lld.im.codec.pack.message;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-01
 * @Description: ack消息类
 * @Version: 1.0
 */

@Data
public class ChatMessageAck {

    private String messageId;

    private Long messageSequence;   // 消息的序列号

    public ChatMessageAck(String messageId) {
        this.messageId = messageId;
    }

    public ChatMessageAck(String messageId, Long messageSequence) {
        this.messageId = messageId;
        this.messageSequence = messageSequence;
    }
}
