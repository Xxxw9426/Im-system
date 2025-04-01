package com.lld.im.tcp.publish;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.Message;
import com.lld.im.common.constant.Constants;
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
    public static void sendMessage(Message message,Integer command) {
        Channel channel = null;
        String channelName= Constants.RabbitConstants.Im2MessageService;
        try{
            // 拿channelName获取我们的channel
            channel=MqFactory.getChannel(channelName);
            // 将Message对象中的消息体转化为JSON字符串
            JSONObject o = (JSONObject) JSON.toJSON(message.getMessagePack());
            // 将逻辑层所需的一些参数加入其中
            o.put("command", command);
            o.put("clientType",message.getMessageHeader().getClientType());
            o.put("imei",message.getMessageHeader().getImei());
            o.put("appId",message.getMessageHeader().getAppId());

            // 调用basicPublish()投递消息
            // 这个方法的参数：1.交换机名称(暂时使用channelName代替),2.routingKey(这里使用没有routingKey的模式),3.拓展字段,4.数据
            channel.basicPublish(channelName,"",null, o.toJSONString().getBytes());

        } catch(Exception e) {
            log.error("发送消息出现异常：{}",e.getMessage());
        }
    }
}
