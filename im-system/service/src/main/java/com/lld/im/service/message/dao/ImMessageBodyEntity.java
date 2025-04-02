package com.lld.im.service.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: Chackylee
 * @description:  单聊消息实体的数据库实体类对象
 **/
@Data
@TableName("im_message_body")
public class ImMessageBodyEntity {

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
