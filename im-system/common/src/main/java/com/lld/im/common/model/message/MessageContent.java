package com.lld.im.common.model.message;

import com.lld.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-01
 * @Description: 用来存储我们接收到的IM层传来的单聊消息类供我们服务层使用的实体类
 * @Version: 1.0
 */

@Data
public class MessageContent extends ClientInfo {

    // 消息id
    private String messageId;

    // 消息发送者
    private String fromId;

    // 消息接收者
    private String toId;

    // 消息体
    private String messageBody;

    // 消息主键标识
    private Long messageKey;

    // 客户端发送消息的时间
    private Long messageTime;

    // 拓展字段
    private String extra;
}
