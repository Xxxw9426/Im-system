package com.lld.im.service.group.model.req;

import com.baomidou.mybatisplus.annotation.TableId;
import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 批量导入群组的请求实体类
 * @Version: 1.0
 */

@Data
public class ImportGroupReq extends RequestBase {

    private String groupId;

    //群主id
    private String ownerId;

    //群类型 1私有群（类似微信） 2公开群(类似qq）
    private Integer groupType;

    @NotBlank(message = "群名称不能为空")
    private String groupName;

    private Integer mute;// 是否全员禁言，0 不禁言；1 全员禁言。

    //    申请加群选项包括如下几种：
    //    0 表示禁止任何人申请加入
    //    1 表示需要群主或管理员审批
    //    2 表示允许无需审批自由加入群组
    private Integer applyJoinType;

    private String introduction;//群简介

    private String notification;//群公告

    private String photo;//群头像

    private Integer maxMemberCount;//群成员上限

    private Integer status;//群状态 0正常 1解散

    private Long sequence;

    private Long createTime;

    private Long updateTime;

    private String extra;
}
