package com.lld.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: Chackylee
 * @description: 单聊消息索引的数据库实体类
 **/
@Data
@TableName("im_message_history")
public class ImMessageHistoryEntity {

    private Integer appId;

    private String fromId;

    private String toId;

    private String ownerId;

    /** messageBodyId*/
    private Long messageKey;
    /** 序列号*/
    private Long sequence;

    private String messageRandom;

    private Long messageTime;

    private Long createTime;

}
