package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-03
 * @Description: 删除所有好友关系链请求实体类
 * @Version: 1.0
 */

@Data
public class DeleteAllFriendReq extends RequestBase {

    // 删除fromId的所有好友关系链
    @NotBlank(message = "fromId不能为空！")
    private String fromId;

}
