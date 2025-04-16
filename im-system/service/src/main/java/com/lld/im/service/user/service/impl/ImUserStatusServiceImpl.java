package com.lld.im.service.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.pack.user.UserCustomStatusChangeNotifyPack;
import com.lld.im.codec.pack.user.UserStatusChangeNotifyPack;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.command.Command;
import com.lld.im.common.enums.command.UserEventCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.common.model.UserSession;
import com.lld.im.service.friendship.service.ImFriendShipService;
import com.lld.im.service.user.model.UserStatusChangeNotifyContent;
import com.lld.im.service.user.model.req.PullFriendOnlineStatusReq;
import com.lld.im.service.user.model.req.PullUserOnlineStatusReq;
import com.lld.im.service.user.model.req.SetUserCustomerStatusReq;
import com.lld.im.service.user.model.req.SubscribeUserOnlineStatusReq;
import com.lld.im.service.user.model.resp.UserOnlineStatusResp;
import com.lld.im.service.user.service.ImUserStatusService;
import com.lld.im.service.utils.MessageProducer;
import com.lld.im.service.utils.UserSessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-10
 * @Description:
 * @Version: 1.0
 */
@Service
public class ImUserStatusServiceImpl implements ImUserStatusService {

    @Autowired
    UserSessionUtils userSessionUtils;


    @Autowired
    MessageProducer messageProducer;


    @Autowired
    ImFriendShipService imFriendShipService;


    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 处理用户在线状态发生变化的方法
     * 首先将当前用户下线的消息发给该用户其他在线的客户端
     * 然后再发送给好友和订阅了当前用户的人
     * @param content
     */
    @Override
    public void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content) {
        // 获取当前用户的所有session客户端
        List<UserSession> userSessions = userSessionUtils.getUserSessions(content.getAppId(), content.getUserId());
        // 封装要发送同步消息的实体类
        UserStatusChangeNotifyPack pack = new UserStatusChangeNotifyPack();
        BeanUtils.copyProperties(content, pack);
        pack.setClient(userSessions);
        // 发送给自己的其他在线端
        syncSender(pack, content.getUserId(), content);
        // 再发送给好友和订阅了当前用户的人
        dispatcher(pack, content.getUserId(), content.getAppId());
    }


    /**
     * 将用户当前端在线状态发生变化的消息发送给用户的其他在线端
     * @param pack
     * @param userId
     * @param clientInfo
     */
    private void syncSender(Object pack, String userId, ClientInfo clientInfo) {
        messageProducer.sendToUserExceptClient(userId, UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC, pack, clientInfo);
    }


    /**
     * 将用户当前端在线状态发生变化的消息发送给好友和订阅了当前用户的人的方法
     * @param pack
     * @param userId
     * @param appId
     */
    private void dispatcher(Object pack,String userId,Integer appId) {
        // 获取当前用户的所有好友的id
        List<String> allFriendId = imFriendShipService.getAllFriendId(userId, appId);
        // 发送给当前用户的所有好友
        for(String id:allFriendId) {
            messageProducer.sendToUser(id,UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY,pack,appId);
        }
        // 发送给订阅了当前用户的人
        String userKey=appId+":"+ Constants.RedisConstants.subscribe +":"+userId;
        Set<Object> keys = stringRedisTemplate.opsForHash().keys(userKey);
        // 获得当前订阅了自己的所有用户
        // 遍历
        for(Object key:keys) {
            String filed=(String)key;
            Long expire = Long.valueOf((String)stringRedisTemplate.opsForHash().get(userKey, filed));
            if(expire>0 && expire<System.currentTimeMillis()) {   // 过期时间大于0并且过期时间小于当前时间
                // 通知给对方
                messageProducer.sendToUser(filed,UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY,pack,appId);
            } else {   // 说明对方的订阅已经过期，删除该缓存
                stringRedisTemplate.opsForHash().delete(userKey, filed);
            }
        }
    }


    /***
     * 订阅用户在线状态
     * @param req
     */
    @Override
    public void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req) {
        // 使用Redis中的hash来存每个人被谁订阅了的人员列表
        Long subExpireTime=0L;
        if(req!=null && req.getSubTime()>0) {
            subExpireTime=System.currentTimeMillis()+req.getSubTime();    // 计算订阅过期时间
        }
        for(String beSubUserId:req.getSubUserId()) {
            String userKey=req.getAppId()+":"+Constants.RedisConstants.subscribe +":"+beSubUserId;
            stringRedisTemplate.opsForHash().put(userKey,req.getOperator(),subExpireTime.toString());
        }
    }


    /***
     * 设置用户自身的在线状态
     * @param req
     */
    @Override
    public void setUserCustomerStatus(SetUserCustomerStatusReq req) {
        // 写入Redis
        UserCustomStatusChangeNotifyPack userCustomStatusChangeNotifyPack = new UserCustomStatusChangeNotifyPack();
        userCustomStatusChangeNotifyPack.setCustomStatus(req.getCustomStatus());
        userCustomStatusChangeNotifyPack.setCustomText(req.getCustomText());
        userCustomStatusChangeNotifyPack.setUserId(req.getUserId());
        stringRedisTemplate.opsForValue().set(req.getAppId()
                        +":"+ Constants.RedisConstants.userCustomerStatus + ":" + req.getUserId()
                , JSONObject.toJSONString(userCustomStatusChangeNotifyPack));

        // 再分发给当前用户的其他在线端和好友以及订阅了当前用户的人
        syncSender(userCustomStatusChangeNotifyPack,
                req.getUserId(),new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        dispatcher(userCustomStatusChangeNotifyPack,req.getUserId(),req.getAppId());
    }


    /***
     *  拉取当前用户的所有好友的在线状态
     * @param req
     * @return
     */
    @Override
    public Map<String, UserOnlineStatusResp> queryFriendOnlineStatus(PullFriendOnlineStatusReq req) {
        List<String> allFriendId = imFriendShipService.getAllFriendId(req.getOperator(), req.getAppId());
        return getUserOnlineStatus(allFriendId,req.getAppId());
    }


    /***
     * 拉取传入列表中的用户的在线状态
     * @param req
     * @return
     */
    @Override
    public Map<String, UserOnlineStatusResp> queryUserOnlineStatus(PullUserOnlineStatusReq req) {
        return getUserOnlineStatus(req.getUserList(),req.getAppId());
    }


    /***
     * 获取传入列表中的用户的在线状态
     * @param userId
     * @param appId
     * @return
     */
    private Map<String, UserOnlineStatusResp> getUserOnlineStatus(List<String> userId,Integer appId){

        Map<String, UserOnlineStatusResp> result = new HashMap<>(userId.size());
        // 遍历传入的列表
        for (String uid : userId) {
            // 查询每一个用户的服务端在线状态和客户端在redis中的在线状态和在线状态码
            UserOnlineStatusResp resp = new UserOnlineStatusResp();
            List<UserSession> userSession = userSessionUtils.getUserSessions(appId, uid);
            resp.setSession(userSession);
            String userKey = appId + ":" + Constants.RedisConstants.userCustomerStatus + ":" + uid;
            String s = stringRedisTemplate.opsForValue().get(userKey);
            if(StringUtils.isNotBlank(s)){
                JSONObject parse = (JSONObject) JSON.parse(s);
                resp.setCustomText(parse.getString("customText"));
                resp.setCustomStatus(parse.getInteger("customStatus"));
            }
            result.put(uid,resp);
        }
        return result;
    }
}
