package com.lld.im.service.friendship.model.callback;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 删除好友功能的之后回调方法所需参数的实体类
 * @Version: 1.0
 */

@Data
public class DeleteFriendAfterCallbackDto {

    // 请求删除好友的用户id
    private String fromId;

    // 要删除的好友的id
    private String toId;
}
