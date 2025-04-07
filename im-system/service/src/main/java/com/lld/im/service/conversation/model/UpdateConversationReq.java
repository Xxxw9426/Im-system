package com.lld.im.service.conversation.model;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-07
 * @Description: 修改会话的请求实体类
 * @Version: 1.0
 */

@Data
public class UpdateConversationReq extends RequestBase {

    private String conversationId;    // 会话Id

    private Integer isMute;           // 是否免打扰

    private Integer isTop;            // 是否置顶

    private String fromId;           // 发起请求的用户id
}
