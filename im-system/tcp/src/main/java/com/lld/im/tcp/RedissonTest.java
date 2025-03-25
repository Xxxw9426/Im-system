package com.lld.im.tcp;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description: redis测试类
 * @Version: 1.0
 */

public class RedissonTest {
    public static void main(String[] args) {
        Config config=new Config();
        config.useSingleServer().setAddress("redis://192.168.88.130:6379").setPassword("942633");
        StringCodec codec=new StringCodec();
        config.setCodec(codec);
        RedissonClient redissonClient= Redisson.create(config);

        // 最简单的获取一个key的值
      /*RBucket<Object> im = redissonClient.getBucket("im");
        System.out.println(im.get());
        im.set("im");
        System.out.println(im.get());*/

        // 基于map的set和get
        /*RMap<Object, Object> imMap = redissonClient.getMap("imMap");
        String client = (String) imMap.get("client");
        System.out.println(client);
        imMap.put("client","webClient");
        System.out.println(imMap.get("client"));*/

        // 消息的发布和订阅
        /*RTopic topic = redissonClient.getTopic("topic");
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence charSequence, String s) {
                System.out.println("收到消息："+s);
            }
        });
        topic.publish("hello lld");*/
    }
}
