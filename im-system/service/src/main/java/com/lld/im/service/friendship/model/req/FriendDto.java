package com.lld.im.service.friendship.model.req;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 好友实体类
 * @Version: 1.0
 */

@Data
public class FriendDto {

    // 要添加的好友id
    private String toId;

    // 备注
    private String remark;

    // 来源
    private String addSource;

    // 拓展
    private String extra;

    // 添加留言
    private String addWording;
}
