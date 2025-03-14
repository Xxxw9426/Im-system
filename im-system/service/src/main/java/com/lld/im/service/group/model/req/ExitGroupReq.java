package com.lld.im.service.group.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-13
 * @Description: 退出群组的请求实体类
 * @Version: 1.0
 */

@Data
public class ExitGroupReq extends RequestBase {

    @NotBlank(message = "群组id不能为空！")
    private String groupId;
}
