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

    public ChatMessageAck(String messageId) {
        this.messageId = messageId;
    }
}
