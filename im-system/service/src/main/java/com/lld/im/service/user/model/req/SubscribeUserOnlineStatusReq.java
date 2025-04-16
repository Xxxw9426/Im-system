package com.lld.im.service.user.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-13
 * @Description: 订阅用户在线状态的请求实体类
 * @Version: 1.0
 */

@Data
public class SubscribeUserOnlineStatusReq extends RequestBase {

    private List<String> subUserId;      // 订阅的id

    private Long subTime;     // 订阅时间
}
