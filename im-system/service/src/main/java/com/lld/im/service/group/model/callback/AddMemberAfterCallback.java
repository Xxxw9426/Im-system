package com.lld.im.service.group.model.callback;

import com.lld.im.service.group.model.resp.AddMemberResp;
import lombok.Data;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 添加群成员功能的之后回调的参数的实体类
 * @Version: 1.0
 */

@Data
public class AddMemberAfterCallback {

    // 添加群成员的群id
    private String groupId;

    // 群类型
    private Integer groupType;

    // 操作人
    private String operator;

    // 添加的用户id集合
    private List<AddMemberResp> memberId;
}
