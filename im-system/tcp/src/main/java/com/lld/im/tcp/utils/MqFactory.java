package com.lld.im.tcp.utils;

import com.lld.im.codec.config.BootstrapConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-23
 * @Description: 在该类中初始化RabbitMQ
 * @Version: 1.0
 */

public class MqFactory {

    private static ConnectionFactory factory=null;


    private static Channel defalutChannel;


    private static ConcurrentHashMap<String,Channel> channelMap=new ConcurrentHashMap<>();


    // 初始化RabbitMQ的方法
    public static void init(BootstrapConfig.Rabbitmq rabbitmq) {

        // 当factory为空的时候才需要初始化
        if(factory==null) {
            // 读取配置文件中配置的信息，并设置进去
            factory=new ConnectionFactory();
            factory.setHost(rabbitmq.getHost());
            factory.setPort(rabbitmq.getPort());
            factory.setUsername(rabbitmq.getUserName());
            factory.setPassword(rabbitmq.getPassword());
            factory.setVirtualHost(rabbitmq.getVirtualHost());
        }

    }


    // 获取连接的方法
    public static Connection getConnection() throws IOException, TimeoutException {
        Connection connection=factory.newConnection();
        return connection;
    }


    // 对外暴露获取Channel的方法
    // 一个Connection可以生成多个Channel，因此我们传入一个ChannelName
    // 后序我们可以根据业务的分类，比如用户，群组等等来为其分配不同的Channel，并且根据其ChannelName来操作其业务对应的Channel
    public static Channel getChannel(String channelName) throws IOException, TimeoutException {
        Channel channel = channelMap.get(channelName);
        // 只有当没有从map中获取到所需的Channel时
        // 才需要创建一个新的连接并且创建新的Channel，否则直接返回获取到的Channel
        if(channel==null) {
            channel= getConnection().createChannel();
            channelMap.put(channelName, channel);
        }
        return channel;
    }


}
