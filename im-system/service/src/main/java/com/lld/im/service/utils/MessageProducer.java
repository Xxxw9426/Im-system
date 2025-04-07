package com.lld.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.command.Command;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.lld.im.codec.proto.MessagePack;
import redis.clients.jedis.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 向mq中发送tpc通知消息
 * @Version: 1.0
 */
@Component
public class MessageProducer {

    private static Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    UserSessionUtils userSessionUtils;

    private String queueName= Constants.RabbitConstants.MessageService2Im;


    /***
     * 最简单最底层的向MQ中发送消息的方法
     * @param session
     * @param msg
     * @return
     */
    public boolean sendMessage(UserSession session,Object msg) {
        try{
            logger.info("send message == " + msg);
            // 调用RabbitTemplate发送消息
            /***
             * 参数说明：
             *   1. 消息队列的名称
             *   2. RoutingKey
             *   3. 要发送的消息对象
             */
            rabbitTemplate.convertAndSend(queueName,session.getBrokerId()+"",msg);
            return true;
        } catch(Exception e) {
            logger.error("send error :" + e.getMessage());
            return false;
        }
    }


    /***
     * 对最底层的发送消息的方法进行封装，实现对我们要发送的消息数据以约定好的方式进行包装
     * @param toId
     * @param command
     * @param msg
     * @param session
     * @return
     */
    public boolean sendPack(String toId, Command command,Object msg,UserSession session) {

        // 我们使用MessagePack对象包装我们要发送数据
        MessagePack messagePack = new MessagePack();
        messagePack.setCommand(command.getCommand());
        messagePack.setToId(toId);
        messagePack.setClientType(session.getClientType());
        messagePack.setAppId(session.getAppId());
        messagePack.setImei(session.getImei());

        // 将我们要发送消息转化成JSONObject
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(msg));
        messagePack.setData(jsonObject);

        // 将我们封装好的对象转化为String，传入底层方法进行发送
        String body = JSONObject.toJSONString(messagePack);
        return sendMessage(session,body);
    }


    /***
     * 发送给指定用户所有端的方法
     * TODO 返回值是成功发送的客户端
     * @param toId
     * @param command
     * @param data
     * @param appId
     */
    public List<ClientInfo> sendToUser(String toId,Command command,Object data,Integer appId) {
        // 获取指定用户所有在线的session
        List<UserSession> userSessions = userSessionUtils.getUserSessions(appId, toId);
        List<ClientInfo> list = new ArrayList<>();

        for(UserSession session : userSessions) {
            boolean b = sendPack(toId, command, data, session);
            if(b) {
                list.add(new ClientInfo(session.getAppId(),session.getClientType(),session.getImei()));
            }
        }
        return list;
    }


    /***
     * 发送给指定用户的指定客户端的方法
     * @param toId
     * @param command
     * @param data
     * @param clientInfo
     */
    public void sendToUser(String toId, Command command, Object data, ClientInfo clientInfo) {
        // 获取指定用户的指定端的session
        UserSession session = userSessionUtils.getUserSessions(clientInfo.getAppId(),toId, clientInfo.getClientType(), clientInfo.getImei());
        sendPack(toId,command,data,session);
    }


    /***
     * 发送给指定用户除了某一端的其它端的方法
     * @param toId
     * @param command
     * @param data
     * @param clientInfo
     */
    public void sendToUserExceptClient(String toId,Command command,Object data, ClientInfo clientInfo) {
        // 获取指定用户所有在线的session
        List<UserSession> userSessions = userSessionUtils.getUserSessions(clientInfo.getAppId(), toId);
        for(UserSession session : userSessions) {
            if(!isMatch(session,clientInfo)) {
                sendPack(toId,command,data,session);
            }
        }
    }


    /***
     *  真正给业务逻辑中调用的方法，这个方法会对业务逻辑中的情况再进一步处理，然后调用上面给不同端发送消息的方法
     * @param toId
     * @param clientType
     * @param imei
     * @param command
     * @param data
     * @param appId
     */
    public void sendToUser(String toId,Integer clientType,String imei,Command command,Object data,Integer appId) {

        // 如果在业务方法中是app管理员调用的话则肯定没有imei号。
        // 因此如果imei号和clientType都不为空的话则说明是客户端调用，此时只需要发送给除了当前客户端以外的客户端。
        if(clientType!=null && StringUtils.isNotBlank(imei)) {
            ClientInfo clientInfo = new ClientInfo(appId,clientType,imei);
            sendToUserExceptClient(toId,command,data,clientInfo);
        // 如果是app管理员的话则要发送给所有客户端
        } else {
            sendToUser(toId,command,data,appId);
        }

     }



    // 用来比较当前遍历到的session与指定端的session是否一样的方法
    private boolean isMatch(UserSession sessionDto, ClientInfo clientInfo) {
        return Objects.equals(sessionDto.getAppId(), clientInfo.getAppId())
                && Objects.equals(sessionDto.getImei(), clientInfo.getImei())
                && Objects.equals(sessionDto.getClientType(), clientInfo.getClientType());
    }
}
