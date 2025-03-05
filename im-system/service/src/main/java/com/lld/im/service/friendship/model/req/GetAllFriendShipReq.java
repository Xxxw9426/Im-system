package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-03
 * @Description: 获取所有好友关系链的请求实体类
 * @Version: 1.0
 */

@Data
public class GetAllFriendShipReq extends RequestBase {

    // 获取from者的所有好友关系链
    @NotBlank(message = "fromId不能为空！")
    private String fromId;

}
