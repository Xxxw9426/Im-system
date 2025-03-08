package com.lld.im.service.friendship.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-05
 * @Description: 审批好友申请功能的请求实体类
 * @Version: 1.0
 */

@Data
public class ApproveFriendRequestReq extends RequestBase {

    // 请求被审批的好友申请的主键id
    private Long id;

    // 审批状态(1.同意  2.拒绝)
    private Integer status;
}
