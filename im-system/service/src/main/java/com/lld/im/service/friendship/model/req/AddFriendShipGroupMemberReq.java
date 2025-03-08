package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-06
 * @Description: 添加组内成员的请求实体类
 * @Version: 1.0
 */

@Data
public class AddFriendShipGroupMemberReq extends RequestBase {

    // 拥有该分组的用户的id
    @NotBlank(message = "fromId不能为空")
    private String fromId;

    // 该分组的名称
    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    // 要添加进分组的用户id
    @NotEmpty(message = "请选择用户")
    private List<String> toIds;
}

