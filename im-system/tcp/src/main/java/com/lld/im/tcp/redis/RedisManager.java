package com.lld.im.tcp.redis;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.receiver.UserLoginMessageListener;
import org.redisson.api.RedissonClient;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description: 初始化redisson的实体类
 * @Version: 1.0
 */

public class RedisManager {

    private static RedissonClient redissonClient;

    // 初始化RedissonClient和监听用户登录消息的类的方法
    public static void init(BootstrapConfig config) {
        // 调用已经封装好的类的方法直接根据配置文件创建RedissonClient
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        System.out.println(config.getLim().getRedis().getSingle());
        redissonClient= singleClientStrategy.getRedissonClient(config.getLim().getRedis());
        // 启动监听用户登录消息的类
        UserLoginMessageListener userLoginMessageListener = new UserLoginMessageListener(config.getLim().getLoginModel());
        userLoginMessageListener.listenerUserLogin();
    }

    // 对外暴露的让外界获取RedissonClient的方法
    public static RedissonClient getRedissonClient() {
        return redissonClient;
    }
}

