package com.lld.im.service.group.model.resp;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 导入群成员的响应实体类,拉人入群的响应实体类(如果是APP管理员则执行拉人进群操作，否则只有私有群可以拉人入群)
 * @Version: 1.0
 */

@Data
public class AddMemberResp {

    private String memberId;

    // 加入结果: 0为成功，1为失败，2为已经是群成员
    private Integer result;

    private String resultMessage;
}
