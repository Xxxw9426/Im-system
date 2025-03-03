package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 添加好友的请求实体类
 * @Version: 1.0
 */

@Data
public class AddFriendReq extends RequestBase {

    // 要创建的好友关系中的from者
    @NotBlank(message = "fromId不能为空！")
    private String fromId;

    // 要添加的好友的信息
    private FriendDto toItem;
}
