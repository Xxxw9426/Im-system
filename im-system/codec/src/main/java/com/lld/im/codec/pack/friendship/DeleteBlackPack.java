package com.lld.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 删除黑名单通知报文
 **/
@Data
public class DeleteBlackPack {

    private String fromId;

    private String toId;

    // 消息的序列号
    private Long sequence;
}
