package com.lld.im.codec.pack.user;

import com.lld.im.common.model.UserSession;
import lombok.Data;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-10
 * @Description: 用户在线状态发生变化后由tcp发送给service的数据包实体类
 * @Version: 1.0
 */
@Data
public class UserStatusChangeNotifyPack {

    // appId
    private Integer appId;

    // 在线状态发生变化的用户id
    private String userId;

    // 用户的在线状态变成了什么
    private Integer status;

    private List<UserSession> client;
}
