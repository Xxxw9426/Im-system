package com.lld.im.codec.pack.group;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 解散群通知报文
 **/
@Data
public class DestroyGroupPack {

    private String groupId;

    // 消息的序列号
    private Long sequence;

}
