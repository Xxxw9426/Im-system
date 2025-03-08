package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-06
 * @Description: 创建好友分组的请求实体类
 * @Version: 1.0
 */

@Data
public class AddFriendShipGroupReq extends RequestBase {

    // 请求创建分组的用户id
    @NotBlank(message = "fromId不能为空")
    private String fromId;

    // 分组组名
    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    // 组内成员
    private List<String> toIds;
}
