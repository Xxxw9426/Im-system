package com.lld.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: Chackylee
 * @description: 群聊消息索引的数据库实体类
 **/
@Data
@TableName("im_group_message_history")
public class ImGroupMessageHistoryEntity {

    private Integer appId;

    private String fromId;

    private String groupId;

    /** messageBodyId*/
    private Long messageKey;

    /** 序列号*/
    private Long sequence;

    private String messageRandom;

    private Long messageTime;

    private Long createTime;


}
