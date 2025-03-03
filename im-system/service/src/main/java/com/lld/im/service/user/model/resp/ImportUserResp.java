package com.lld.im.service.user.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-01
 * @Description: 响应批量导入用户资料结果的实体类
 * @Version: 1.0
 */

@Data
public class ImportUserResp {

    // 返回的导入成功的用户Id
    private List<String> successId;

    // 返回的导入失败的用户Id
    private List<String> errorId;
}
