package com.lld.im.codec.pack.conversation;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-07
 * @Description: 更新会话的tcp数据包实体类
 * @Version: 1.0
 */
@Data
public class UpdateConversationPack {

    private String conversationId;          // 会话Id

    private Integer isMute;                 // 是否免打扰

    private Integer isTop;                  // 是否置顶

    private Integer conversationType;        // 会话种类
}
