package com.lld.im.service.user.model.req;

import lombok.Data;

import javax.validation.constraints.NotNull;


/**
 * @author: Chackylee
 * @description:  请求获取IM系统的web地址/tcp地址的请求实体类
 **/
@Data
public class LoginReq {

    @NotNull(message = "用户id不能位空")
    private String userId;

    @NotNull(message = "appId不能为空")
    private Integer appId;

    private Integer clientType;

}
