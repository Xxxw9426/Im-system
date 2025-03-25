package com.lld.im.common.constant;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-19
 * @Description: 维护tcp包中会用到的常量
 * @Version: 1.0
 */

public class Constants {


    /** channel绑定的userId Key*/
    public static final String UserId = "userId";

    /** channel绑定的appId */
    public static final String AppId = "appId";

    /** channel绑定的clientType */
    public static final String ClientType = "clientType";

    /** channel绑定的readTime */
    public static final String ReadTime="readTime";

    /** channel绑定的imei号，imei号用来标识登录的设备，当同一个端进行了重复登录后根据imei号踢人 */
    public static final String Imei="imei";


    /** 维护zookeeper中父节点常量 */
    public static final String ImCoreZkRoot = "/im-coreRoot";

    public static final String ImCoreZkRootTcp = "/tcp";

    public static final String ImCoreZkRootWeb = "/web";


    /** 与Redis有关的常量 */
    public static class RedisConstants{

        /**
         *  Redis中存储用户Session所对应的key，格式：appId + UserSessionConstants + userId
         */
        public static final String UserSessionConstants=":userSession:";


        /**
         * 用户上线通知channel
         */
        public static final String UserLoginChannel = "signal/channel/LOGIN_USER_INNER_QUEUE";

    }


    /** 与RabbitMQ有关的常量 */
    public static class RabbitConstants{

        /** IM服务给用户服务投递的消息 */
        public static final String Im2UserService = "pipeline2UserService";

        /** IM服务给消息服务投递的消息 */
        public static final String Im2MessageService = "pipeline2MessageService";

        /** IM服务给群组服务投递的消息 */
        public static final String Im2GroupService = "pipeline2GroupService";

        /** IM服务给关系链服务投递的消息 */
        public static final String Im2FriendshipService = "pipeline2FriendshipService";

        /** 消息服务给IM服务投递的消息 */
        public static final String MessageService2Im = "messageService2Pipeline";

        /** 群组服务给IM服务投递的消息 */
        public static final String GroupService2Im = "GroupService2Pipeline";

        /** 关系链服务给IM服务投递的消息 */
        public static final String FriendShip2Im = "friendShip2Pipeline";


        public static final String StoreP2PMessage = "storeP2PMessage";


        public static final String StoreGroupMessage = "storeGroupMessage";


    }


}
