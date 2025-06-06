package com.lld.im.common.model;

import lombok.Data;

/**
 * @author: Chackylee
 * @description: 接收增量同步时客户端的请求
 **/
@Data
public class SyncReq extends RequestBase {

    //客户端最大seq
    private Long lastSequence;

    //一次拉取多少
    private Integer maxLimit;

}
