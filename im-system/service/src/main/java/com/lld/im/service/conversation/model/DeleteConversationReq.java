package com.lld.im.service.conversation.model;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-07
 * @Description: 删除会话的请求实体类
 * @Version: 1.0
 */

@Data
public class DeleteConversationReq extends RequestBase {

    @NotBlank(message = "会话Id不能为空")
    private String conversationId;    // 会话Id

    @NotBlank(message = "fromId不能为空")
    private String fromId;       // 发起删除会话的用户id
}
