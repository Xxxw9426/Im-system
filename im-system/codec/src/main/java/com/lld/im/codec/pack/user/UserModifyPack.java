package com.lld.im.codec.pack.user;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 数据多端同步中更新用户信息时发送tcp请求时的数据包，其内容为用户更新的数据
 * @Version: 1.0
 */

@Data
public class UserModifyPack {

    // 用户id
    private String userId;

    // 用户名称
    private String nickName;

    // 用户密码
    private String password;

    // 头像
    private String photo;

    // 性别
    private String userSex;

    // 个性签名
    private String selfSignature;

    // 加好友验证类型（Friend_AllowType） 1需要验证
    private Integer friendAllowType;

}
