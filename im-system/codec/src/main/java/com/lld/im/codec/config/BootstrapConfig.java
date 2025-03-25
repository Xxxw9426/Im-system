package com.lld.im.codec.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-16
 * @Description: 实体类，用来接收tcp包中配置文件的内容
 * @Version: 1.0
 */

@Data
public class BootstrapConfig {


    private TcpConfig lim;


    @Data
    public static class TcpConfig{

        private Integer tcpPort;  // tcp绑定的端口号

        private Integer webSocketPort;    // webSocket 绑定的端口号

        private boolean enableWebSocket;   // 是否启用webSocket

        private Integer bossThreadSize;   // boss线程，默认=1

        private Integer workThreadSize;   // work线程

        private Long heartBeatTime;       // 心跳超时时间 单位毫秒

        // redis配置
        private RedisConfig redis;

        // rabbitmq配置
        private Rabbitmq rabbitmq;

        // zk配置
        private ZkConfig zkConfig;

        // brokerId 服务端id,根据brokerId来区分我们的服务
        private Integer brokerId;

        // 多端登录多端同步模式
        private Integer loginModel;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {

        /**
         * 单机模式：single 哨兵模式：sentinel 集群模式：cluster
         */
        private String mode;
        /**
         * 数据库
         */
        private Integer database;
        /**
         * 密码
         */
        private String password;
        /**
         * 超时时间
         */
        private Integer timeout;
        /**
         * 最小空闲数
         */
        private Integer poolMinIdle;
        /**
         * 连接超时时间(毫秒)
         */
        private Integer poolConnTimeout;
        /**
         * 连接池大小
         */
        private Integer poolSize;

        /**
         * redis单机配置
         */
        private RedisSingle single;

    }

    /**
     * redis单机配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisSingle {
        /**
         * 地址
         */
        private String address;
    }


    /**
     * rabbitmq哨兵模式配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbitmq {
        private String host;

        private Integer port;

        private String virtualHost;

        private String userName;

        private String password;
    }


    /**
     * zookeeper配置
     */
    @Data
    public static class ZkConfig {
        /**
         * zk连接地址
         */
        private String zkAddr;

        /**
         * zk连接超时时间
         */
        private Integer zkConnectTimeOut;
    }

}
