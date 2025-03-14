package com.lld.im.service.user.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;


@Data
public class DeleteUserReq extends RequestBase {

    // 要删除的所有用户的id
    @NotEmpty(message = "用户id不能为空")
    private List<String> userId;
}
