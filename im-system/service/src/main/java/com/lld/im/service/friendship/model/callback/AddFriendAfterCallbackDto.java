package com.lld.im.service.friendship.model.callback;

import com.lld.im.service.friendship.model.req.FriendDto;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 添加好友功能的之后回调方法所需参数的实体类
 * @Version: 1.0
 */

@Data
public class AddFriendAfterCallbackDto {

    // 请求添加好友的用户id
    private String fromId;

    // 要添加的好友的信息
    private FriendDto toItem;
}
