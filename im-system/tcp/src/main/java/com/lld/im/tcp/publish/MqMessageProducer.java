package com.lld.im.tcp.publish;


import com.alibaba.fastjson.JSONObject;
import com.lld.im.tcp.utils.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-23
 * @Description: 用来向逻辑层投递消息的组件工具类,当我们需要发送消息的时候,调用该类中的方法即可。
 * @Version: 1.0
 */

@Slf4j
public class MqMessageProducer {

    /***
     *  投递消息：参数为我们要发送的消息对象
     * @param message
     */
    public static void sendMessage(Object message) {
        Channel channel = null;
        String channelName="";
        try{

            // 拿channelName获取我们的channel
            channel=MqFactory.getChannel(channelName);
            // 这个方法的参数：1.交换机名称(暂时使用channelName代替),2.routingKey(这里使用没有routingKey的模式),3.拓展字段,4.数据
            channel.basicPublish(channelName,"",null, JSONObject.toJSONString(message).getBytes());

        } catch(Exception e) {
            log.error("发送消息出现异常：{}",e.getMessage());
        }
    }
}
