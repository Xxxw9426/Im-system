package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-05
 * @Description: 已读好友申请列表的请求实体类
 * @Version: 1.0
 */

@Data
public class ReadFriendShipRequestReq extends RequestBase {

    @NotBlank(message = "用户ID不能为空")
    private String fromId;

}
