package com.lld.im.service.group.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author: Chackylee
 * @description: 更新群成员信息的请求实体类
 **/
@Data
public class UpdateGroupMemberReq extends RequestBase {

    @NotBlank(message = "群id不能为空")
    private String groupId;

    // 被更新的群成员id
    @NotBlank(message = "memberId不能为空")
    private String memberId;

    // 群昵称
    private String alias;

    // 身份
    private Integer role;

    // 其他
    private String extra;

}
