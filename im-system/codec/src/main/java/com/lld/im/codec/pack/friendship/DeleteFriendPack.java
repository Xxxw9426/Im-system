package com.lld.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 删除好友通知报文
 **/
@Data
public class DeleteFriendPack {

    private String fromId;

    private String toId;

    // 消息的序列号
    private Long sequence;
}
