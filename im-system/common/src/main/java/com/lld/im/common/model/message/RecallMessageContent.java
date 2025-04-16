package com.lld.im.common.model.message;

import com.lld.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @author: Chackylee
 * @description:  撤回消息的数据包
 **/
@Data
public class RecallMessageContent extends ClientInfo {

    private Long messageKey;   // 要撤回的消息的messageKey

    private String fromId;

    private String toId;

    private Long messageTime;

    private Long messageSequence;     // 消息sequence

    private Integer conversationType;   // 会话类型


//    {
//        "messageKey":419455774914383872,
//            "fromId":"lld",
//            "toId":"lld4",
//            "messageTime":"1665026849851",
//            "messageSequence":2,
//            "appId": 10000,
//            "clientType": 1,
//            "imei": "web",
//    "conversationType":0
//    }
}
