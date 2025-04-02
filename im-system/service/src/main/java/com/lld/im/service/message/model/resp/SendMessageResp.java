package com.lld.im.service.message.model.resp;

import lombok.Data;

/**
 * @description: 发送消息请求的返回实体类
 * @author: lld
 * @version: 1.0
 */
@Data
public class SendMessageResp {

    private Long messageKey;

    private Long messageTime;

}
