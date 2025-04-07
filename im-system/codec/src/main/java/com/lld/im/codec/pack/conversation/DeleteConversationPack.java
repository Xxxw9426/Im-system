package com.lld.im.codec.pack.conversation;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-07
 * @Description: 删除会话的tcp数据包类
 * @Version: 1.0
 */
@Data
public class DeleteConversationPack {

    private String conversationId;    // 会话Id

}
