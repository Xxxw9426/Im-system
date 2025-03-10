package com.lld.im.service.group.model.resp;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-10
 * @Description: 获取单个用户在单个群聊中身份的返回类
 * @Version: 1.0
 */

@Data
public class GetRoleInGroupResp {

    // group-member表的主键
    private Long groupMemberId;

    // 当前用户的id
    private String memberId;

    // 当前用户在群聊中的角色
    private Integer role;

    // 当前用户的禁言结束期限
    private Long speakDate;
}
