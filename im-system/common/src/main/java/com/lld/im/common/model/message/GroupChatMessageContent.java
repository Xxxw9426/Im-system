package com.lld.im.common.model.message;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-01
 * @Description:  用来存储我们接收到的IM层传来的群聊消息类供我们服务层使用的实体类
 * @Version: 1.0
 */

@Data
public class GroupChatMessageContent extends MessageContent {

    // 群组id
    private String groupId;
}

