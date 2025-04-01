package com.lld.im.codec.pack.friendship;

import lombok.Data;


/**
 * @author: Chackylee
 * @description: 更新好友tcp通知数据包
 **/
@Data
public class UpdateFriendPack {

    public String fromId;

    private String toId;

    private String remark;

    private Long sequence;
}
