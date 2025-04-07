package com.lld.im.codec.pack.message;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-05
 * @Description: 当消息接收方不在线时由服务端发起的消息确认类
 * @Version: 1.0
 */
@Data
public class MessageReceiveServerAckPack {

    // 消息id/key
    private Long messageKey;

    // 消息发送者
    private String fromId;

    // 消息接收者
    private String toId;

    // 消息的序列号
    private Long messageSequence;

    // 是否是由服务端发起的
    private Boolean serverSend;

}
