package com.lld.im.tcp.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.common.model.UserSession;
import com.lld.im.tcp.redis.RedisManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description: 用来在用户登录与登出业务中管理用户的Channel的类
 * @Version: 1.0
 */

public class SessionSocketHolder {

    // TODO 任何channels的读取或者添加都是向这个map中添加元素或者取出元素
    private static final Map<UserClientDto,NioSocketChannel> CHANNELS = new ConcurrentHashMap<UserClientDto, NioSocketChannel>();


    // 存入Channel
    public static void put(Integer appId,String userId,Integer clientType,String imei,
                           NioSocketChannel channel) {
        UserClientDto user=new UserClientDto();
        user.setImei(imei);
        user.setAppId(appId);
        user.setUserId(userId);
        user.setClientType(clientType);
        CHANNELS.put(user,channel);
    }


    // 获得Channel
    public static NioSocketChannel get(Integer appId,String userId,String imei,
                                       Integer clientType) {
        UserClientDto user=new UserClientDto();
        user.setImei(imei);
        user.setAppId(appId);
        user.setUserId(userId);
        user.setClientType(clientType);
        return CHANNELS.get(user);
    }


    // 根据用户信息移除对应的channel
    public static void remove(Integer appId,String userId,String imei,
                              Integer clientType) {
        UserClientDto user=new UserClientDto();
        user.setImei(imei);
        user.setAppId(appId);
        user.setUserId(userId);
        user.setClientType(clientType);
        CHANNELS.remove(user);
    }


    // 移除传入的channel
    public static void remove(NioSocketChannel channel) {
        CHANNELS.entrySet().stream().filter(entity->entity.getValue()==channel)
                .forEach(entry -> CHANNELS.remove(entry.getKey()));
    }


    // 用户退出登录，即删除用户对应的channel和Redis中的session
    public static void removeUserSession(NioSocketChannel channel) {

        // 删除当前用户对应的Channel
        // 首先获取用户id，appId和clientType，再根据这三者删除对应的Channel
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();
        // 移除Channel
        remove(appId,userId,imei,clientType);

        // 删除Redis中对应的Session
        // 获取RedissonClient将当前对象从redis中移除
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        // 删除Redis中用户Session
        map.remove(clientType+":"+imei);
        // 关闭Channel
        channel.close();

    }


    // 用户离线，即删除用户对应的channel并且修改Redis中用户session中的在线状态为离线
    public static void offlineUserSession(NioSocketChannel channel) {

        // 删除当前用户对应的Channel
        // 首先获取用户id，appId和clientType，再根据这三者删除对应的Channel
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();
        // 移除Channel
        remove(appId,userId,imei,clientType);

        // 获取Redis中对应的Session并修改当前用户的在线状态为离线
        // 获取RedissonClient进行修改操作
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        // 从Redis中获取此时的userSession对象
        String sessionStr = map.get(clientType.toString());
        if(!StringUtils.isBlank(sessionStr)) {
            // 将获取到的数据转化为对象
            UserSession userSession = JSONObject.parseObject(sessionStr, UserSession.class);
            // 修改当前用户的在线状态
            userSession.setConnectState(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            // 重新写入Redis
            map.put(clientType.toString()+":"+imei, JSONObject.toJSONString(userSession));
        }
        // 关闭channel
        channel.close();
    }


    // 当本服务器监听到用户登录消息后，获取本服务器中当前登录用户的所有channel
    public static List<NioSocketChannel> get(Integer appId,String id) {
        Set<UserClientDto> channelInfos = CHANNELS.keySet();
        List<NioSocketChannel> channels = new ArrayList<NioSocketChannel>();

        for(UserClientDto user:channelInfos) {
            if(user.getAppId().equals(appId) && user.getUserId().equals(id)) {
                channels.add(CHANNELS.get(user));
            }
        }
        return channels;
    }
}
