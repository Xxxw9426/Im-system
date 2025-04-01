package com.lld.im.service.group.model.callback;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 解散群组功能的之后回调所需参数的实体类
 * @Version: 1.0
 */

@Data
public class DestroyGroupCallbackDto {

    private String groupId;
}
