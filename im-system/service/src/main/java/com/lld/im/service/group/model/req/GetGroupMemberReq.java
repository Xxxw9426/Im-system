package com.lld.im.service.group.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-14
 * @Description: 获取群组内群成员信息的请求实体类
 * @Version: 1.0
 */

@Data
public class GetGroupMemberReq  extends RequestBase {

    @NotBlank(message = "群组id不能为空！")
    private String groupId;

    @NotBlank(message = "用户id不能为空！")
    private String memberId;

}
