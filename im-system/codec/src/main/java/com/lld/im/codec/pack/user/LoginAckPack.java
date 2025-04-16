package com.lld.im.codec.pack.user;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-10
 * @Description: 用户登录成功后向用户响应的ack消息实体类
 * @Version: 1.0
 */
@Data
public class LoginAckPack {

    private String userId;
}
