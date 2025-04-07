package com.lld.im.tcp.feign;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.model.message.CheckSendMessageReq;
import feign.Headers;
import feign.RequestLine;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-02
 * @Description:
 * @Version: 1.0
 */

public interface FeignMessageService {

    @Headers({"content-Type: application/json","Accept: application/json"})
    @RequestLine("POST /message/checkSend")
    public ResponseVO checkSendMessage(CheckSendMessageReq req);
}
