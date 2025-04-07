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
         * userSign，格式：appId:userSign:
         */
        public static final String userSign = "userSign";


        /**
         *  Redis中存储用户Session所对应的key，格式：appId + UserSessionConstants + userId
         */
        public static final String UserSessionConstants=":userSession:";


        /**
         * 用户上线通知channel
         */
        public static final String UserLoginChannel = "signal/channel/LOGIN_USER_INNER_QUEUE";


        /**
         * 缓存客户端消息防重，格式： appId + :cacheMessage: + messageId
         */
        public static final String cacheMessage = "cacheMessage";

        /**
         * 存储离线消息时的key值的一部分
         */
        public static final String OfflineMessage = "offlineMessage";

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

        /** MQ异步持久化单聊消息  */
        public static final String StoreP2PMessage = "storeP2PMessage";


        public static final String StoreGroupMessage = "storeGroupMessage";


    }


    /** 与业务回调有关的常量 */
    public static class CallbackCommand{

        public static final String ModifyUserAfter = "user.modify.after";

        public static final String CreateGroupAfter = "group.create.after";

        public static final String UpdateGroupAfter = "group.update.after";

        public static final String DestroyGroupAfter = "group.destroy.after";

        public static final String TransferGroupAfter = "group.transfer.after";

        public static final String GroupMemberAddBefore = "group.member.add.before";

        public static final String GroupMemberAddAfter = "group.member.add.after";

        public static final String GroupMemberDeleteAfter = "group.member.delete.after";

        public static final String AddFriendBefore = "friend.add.before";

        public static final String AddFriendAfter = "friend.add.after";

        public static final String UpdateFriendBefore = "friend.update.before";

        public static final String UpdateFriendAfter = "friend.update.after";

        public static final String DeleteFriendAfter = "friend.delete.after";

        public static final String AddBlackAfter = "black.add.after";

        public static final String DeleteBlack = "black.delete";

        public static final String SendMessageAfter = "message.send.after";

        public static final String SendMessageBefore = "message.send.before";

    }


    public static class SeqConstants{

        public static final String Message = "messageSeq";

        public static final String GroupMessage = "groupMessageSeq";

    }


}
