package com.lld.im.codec.pack.message;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-06
 * @Description: 消息已读信息实体类
 * @Version: 1.0
 */

@Data
public class MessageReadPack {

    private Long messageSequence;   // 消息的序列号，标识我们读到了哪一条消息

    private String fromId;     // 发起消息已读请求的用户id

    private String groupId;    // 群聊id

    private String toId;      // 将这个指令发送给谁

    private Integer conversationType;   // 会话种类

}
