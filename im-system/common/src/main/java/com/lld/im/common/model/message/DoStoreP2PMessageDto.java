package com.lld.im.common.model.message;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-02
 * @Description: 存储单聊消息的实体类
 * @Version: 1.0
 */
@Data
public class DoStoreP2PMessageDto {

    private MessageContent messageContent;

    private ImMessageBody imMessageBody;
}
