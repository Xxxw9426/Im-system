package com.lld.im.common.model.message;

import lombok.Data;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-02
 * @Description: 近似代表单聊消息表实体类
 * @Version: 1.0
 */
@Data
public class ImMessageBody {

    private Integer appId;

    /** messageBodyId*/
    private Long messageKey;    // 消息的唯一标识

    /** messageBody*/
    private String messageBody;    // 消息体中的数据

    private String securityKey;    // 预留字段，表示我们可以对messageBody进行加密，随后使用密钥进行解密

    private Long messageTime;      // 客户端发送消息的时间

    private Long createTime;       // 服务端插入记录的时间

    private String extra;          // 拓展字段

    private Integer delFlag;        // 删除标记
}
