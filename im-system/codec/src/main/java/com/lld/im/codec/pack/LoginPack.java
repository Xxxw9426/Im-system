package com.lld.im.codec.pack;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description: 用来存放用户登录时客户端和服务端之间传输的数据包的实体类
 * @Version: 1.0
 */
@Data
public class LoginPack {

    // 用户登录的时候需要传输自己的用户id就可以发消息，至于用户是否存在我们会在用户发消息时在业务逻辑层进行判断
    private String userId;
}
