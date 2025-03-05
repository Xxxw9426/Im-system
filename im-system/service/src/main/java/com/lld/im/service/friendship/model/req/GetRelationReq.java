package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-03
 * @Description: 获取特定好友关系链的请求实体类
 * @Version: 1.0
 */

@Data
public class GetRelationReq extends RequestBase {

    // 要获取的好友关系中的from者
    @NotBlank(message = "fromId不能为空！")
    private String fromId;


    // 要获取的好友关系中的to者
    @NotBlank(message = "toId不能为空！")
    private String toId;
}
