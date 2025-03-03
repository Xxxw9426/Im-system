package com.lld.im.service.user.model.req;

import com.lld.im.common.model.RequestBase;
import com.lld.im.service.user.dao.ImUserDataEntity;
import lombok.Data;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-01
 * @Description: 批量导入用户资料的请求实体类
 * @Version: 1.0
 */

@Data
public class ImportUserReq extends RequestBase {

    // extends RequestBase
    // private String appId;

    // 批量添加，所以用List存储
    private List<ImUserDataEntity> userData;
}
