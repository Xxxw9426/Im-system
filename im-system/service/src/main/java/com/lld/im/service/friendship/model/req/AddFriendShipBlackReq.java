package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;


// 添加黑名单请求实体类
@Data
public class AddFriendShipBlackReq extends RequestBase {

    // 要添加黑名单的fromId
    @NotBlank(message = "用户id不能为空")
    private String fromId;

    // 被fromId添加为黑名单的toId
    private String toId;
}
