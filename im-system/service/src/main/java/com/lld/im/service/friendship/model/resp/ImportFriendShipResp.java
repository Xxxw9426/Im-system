package com.lld.im.service.friendship.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 导入好友关系链的响应实体类
 * @Version: 1.0
 */

@Data
public class ImportFriendShipResp {

    // 导入成功的用户id集合
    private List<String> successId;

    // 导入失败的用户id集合
    private List<String> errorId;
}
