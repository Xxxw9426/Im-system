package com.lld.im.service.friendship.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// 好友分组数据库表对应的实体类
@Data
@TableName("im_friendship_group")
public class ImFriendShipGroupEntity {

    @TableId(value = "group_id",type = IdType.AUTO)
    private Long groupId;

    private String fromId;

    private Integer appId;

    private String groupName;
    /** 备注*/
    private Long createTime;

    /** 备注*/
    private Long updateTime;

    /** 序列号*/
    private Long sequence;

    private int delFlag;


}
