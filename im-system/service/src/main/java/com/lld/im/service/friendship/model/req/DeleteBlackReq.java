package com.lld.im.service.friendship.model.req;


import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;


// 删除黑名单请求实体类
@Data
public class DeleteBlackReq extends RequestBase {

    // 请求删除黑名单的fromId
    @NotBlank(message = "用户id不能为空")
    private String fromId;

    // 被fromId拉出黑名单的toId
    @NotBlank(message = "好友id不能为空")
    private String toId;

}
