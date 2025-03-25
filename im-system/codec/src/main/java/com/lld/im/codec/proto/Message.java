package com.lld.im.codec.proto;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description: message对象(请求对象)
 * @Version: 1.0
 */
@Data
public class Message {

    // 请求消息中的请求头
    private MessageHeader messageHeader;

    // 请求消息中的消息体
    private Object messagePack;

    @Override
    public String toString() {
        return "Message{" +
                "messageHeader=" + messageHeader +
                ", messagePack=" + messagePack +
                '}';
    }
}
