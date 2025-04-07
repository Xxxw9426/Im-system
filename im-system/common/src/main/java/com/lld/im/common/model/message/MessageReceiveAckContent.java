package com.lld.im.common.model.message;

import com.lld.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-03
 * @Description: 定位消息接受确认状态的实体类
 * @Version: 1.0
 */

@Data
public class MessageReceiveAckContent extends ClientInfo {

    // 消息id/key
    private Long messageKey;

    // 消息发送者
    private String fromId;

    // 消息接收者
    private String toId;

    // 消息的序列号
    private Long messageSequence;
}
