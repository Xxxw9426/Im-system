package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-03
 * @Description: 更新好友关系链的请求实体类
 * @Version: 1.0
 */

@Data
public class UpdateFriendReq extends RequestBase {

    // 要修改的好友关系中的from者
    @NotBlank(message = "fromId不能为空！")
    private String fromId;

    // 要修改的信息以及对应的好友
    private FriendDto toItem;

}
