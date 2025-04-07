package com.lld.im.common.model.message;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-02
 * @Description: 消息前置校验请求实体类
 * @Version: 1.0
 */

@Data
public class CheckSendMessageReq {

    // 消息发送方
    private String fromId;

    // 消息接收方
    private String toId;

    // appId
    private Integer appId;

    // 指令标识
    private Integer command;
}
