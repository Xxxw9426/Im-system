package com.lld.im.service.user.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author: Chackylee
 * @description:
 **/

// 查询单个用户资料的请求类
@Data
public class UserId extends RequestBase {

    @NotEmpty(message = "用户id不能为空！")
    private String userId;

}
