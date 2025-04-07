package com.lld.im.common.model;

import com.lld.im.common.model.message.GroupChatMessageContent;
import com.lld.im.common.model.message.ImMessageBody;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-05
 * @Description: 存储群聊消息的实体类
 * @Version: 1.0
 */
@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBody messageBody;
}
