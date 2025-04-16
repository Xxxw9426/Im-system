package com.lld.im.service.user.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-15
 * @Description: 用户自行设置在线状态的请求实体类
 * @Version: 1.0
 */

@Data
public class SetUserCustomerStatusReq extends RequestBase {

    private String userId;          // 要设置自身在线状态的用户id

    private String customText;       // 在线状态显示字段

    private Integer customStatus;        // 在线状态码

}
