package com.lld.im.service.group.model.req;

import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author: Chackylee
 * @description: 获取群组信息的请求实体类
 **/
@Data
public class GetGroupInfoReq extends RequestBase {

    @NotBlank(message = "群组id不能为空")
    private String groupId;

}
