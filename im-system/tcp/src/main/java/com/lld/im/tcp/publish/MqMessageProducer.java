package com.lld.im.tcp.publish;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.Message;
import com.lld.im.codec.proto.MessageHeader;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.command.CommandType;
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
     * @param command
     */
    public static void sendMessage(Message message,Integer command) {
        Channel channel = null;
        // 根据command判断我们的消息要发送给哪个模块的业务逻辑层
        String com = command.toString();
        String commandSub = com.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(commandSub);
        String channelName = "";
        if(commandType == CommandType.MESSAGE){
            channelName = Constants.RabbitConstants.Im2MessageService;
        }else if(commandType == CommandType.GROUP){
            channelName = Constants.RabbitConstants.Im2GroupService;
        }else if(commandType == CommandType.FRIEND){
            channelName = Constants.RabbitConstants.Im2FriendshipService;
        }else if(commandType == CommandType.USER){
            channelName = Constants.RabbitConstants.Im2UserService;
        }
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


    /***
     * 投递消息：参数为我们要发送的消息对象
     * @param message  可以传入自定义的数据类型
     * @param header    header中包含着我们的路由属性
     * @param command
     */
    public static void sendMessage(Object message, MessageHeader header, Integer command) {
        Channel channel = null;
        // 根据command判断我们的消息要发送给哪个模块的业务逻辑层
        String com = command.toString();
        String commandSub = com.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(commandSub);
        String channelName = "";
        if(commandType == CommandType.MESSAGE){
            channelName = Constants.RabbitConstants.Im2MessageService;
        }else if(commandType == CommandType.GROUP){
            channelName = Constants.RabbitConstants.Im2GroupService;
        }else if(commandType == CommandType.FRIEND){
            channelName = Constants.RabbitConstants.Im2FriendshipService;
        }else if(commandType == CommandType.USER){
            channelName = Constants.RabbitConstants.Im2UserService;
        }
        try{
            // 拿channelName获取我们的channel
            channel=MqFactory.getChannel(channelName);
            // 将Message对象中的消息体转化为JSON字符串
            JSONObject o = (JSONObject) JSON.toJSON(message);
            // 将逻辑层所需的一些参数加入其中
            o.put("command", command);
            o.put("clientType",header.getClientType());
            o.put("imei",header.getImei());
            o.put("appId",header.getAppId());

            // 调用basicPublish()投递消息
            // 这个方法的参数：1.交换机名称(暂时使用channelName代替),2.routingKey(这里使用没有routingKey的模式),3.拓展字段,4.数据
            channel.basicPublish(channelName,"",null, o.toJSONString().getBytes());

        } catch(Exception e) {
            log.error("发送消息出现异常：{}",e.getMessage());
        }
    }
}
