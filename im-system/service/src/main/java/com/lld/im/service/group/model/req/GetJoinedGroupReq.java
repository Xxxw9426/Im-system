package com.lld.im.service.group.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-10
 * @Description: 获取用户加入的所有群组信息的请求实体类
 * @Version: 1.0
 */

@Data
public class GetJoinedGroupReq extends RequestBase {

    @NotBlank(message = "memberId不能为空")
    private String memberId;

    // 传入我们要查询的群聊的种类
    private List<Integer> groupType;
}
