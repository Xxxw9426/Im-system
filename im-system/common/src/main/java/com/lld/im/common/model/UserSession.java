package com.lld.im.common.model;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description: 自定义用户session实体类，存储用户的信息
 * @Version: 1.0
 */
@Data
public class UserSession {
    // 用户id
    private String userId;

    // 应用id
    private Integer appId;

    // 端标识
    private Integer clientType;

    // 当前用户登录在的服务端id
    private Integer brokerId;

    // 当前用户登录在的服务端的ip
    private String brokerHost;

    // sdk版本号
    private Integer version;

    // 连接状态 是否连接正常：1=在线，2=离线
    private Integer connectState;
}
