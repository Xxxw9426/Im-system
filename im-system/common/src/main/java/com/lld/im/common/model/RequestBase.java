package com.lld.im.common.model;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-01
 * @Description: 公共请求类,所有请求都要经过这个类
 * @Version: 1.0
 */

@Data
public class RequestBase {

    private Integer appId;

    // 操作人(可以获取到谁正在调用这个接口)
    private String operator;
}
