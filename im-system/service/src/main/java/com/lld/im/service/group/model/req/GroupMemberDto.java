package com.lld.im.service.group.model.req;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lld.im.common.model.RequestBase;
import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 导入群成员的群成员信息实体类
 * @Version: 1.0
 */

@Data
public class GroupMemberDto extends RequestBase {

    private String groupId;

    //成员id
    private String memberId;

    //群成员类型，0 普通成员, 1 管理员, 2 群主， 3 禁言，4 已经移除的成员
    private Integer role;

    // 禁言到期时间
    private Long speakDate;

    //群昵称
    private String alias;

    //加入时间
    private Long joinTime;

    //离开时间
    private Long leaveTime;

    private String joinType;

    private String extra;
}
