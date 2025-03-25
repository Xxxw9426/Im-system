package com.lld.im.common.model;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-19
 * @Description: 存储用户登录相关信息的实体类
 * @Version: 1.0
 */

@Data
public class UserClientDto {

    // 用户id
    private String userId;

    // 用户当前登录的端类型
    private Integer clientType;

    // appId
    private Integer appId;

    // imei号
    private String imei;
}
