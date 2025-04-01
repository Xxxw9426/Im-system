package com.lld.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-27
 * @Description: 处理用户session相关的工具类
 * @Version: 1.0
 */

@Component
public class UserSessionUtils {

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    /***
     * 获取当前用户所有在线的session
     * @param appId
     * @param userId
     * @return
     */
    public List<UserSession> getUserSessions(Integer appId,String userId) {

        // 首先设定redis中存储userSession所对应的key值
        String userSessionKey=appId+ Constants.RedisConstants.UserSessionConstants+userId;
        // 然后根据key查询到所有的key-value集合对象
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(userSessionKey);
        // 结果集
        List<UserSession> res=new ArrayList<UserSession>();
        // 获取查询到key-value集合的value集合
        Collection<Object> values = map.values();
        // 遍历并且将其转成userSession对象
        for(Object value:values){
            String str=(String)value;
            UserSession userSession = JSONObject.parseObject(str, UserSession.class);
            // 将状态为在线的userSession加入结果集
            if(userSession.getConnectState()== ImConnectStatusEnum.ONLINE_STATUS.getCode()) {
                res.add(userSession);
            }
        }
        return res;

    }


    /***
     * 获取用户指定端的session
     * @param appId
     * @param userId
     * @param clientType
     * @param imei
     * @return
     */
    public UserSession getUserSessions(Integer appId,String userId,
                                             Integer clientType,String imei) {

        // 首先设定redis中存储userSession所对应的key值
        String userSessionKey=appId+ Constants.RedisConstants.UserSessionConstants+userId;
        // 再获取hashKey
        String hashKey=clientType+":"+imei;
        Object o = stringRedisTemplate.opsForHash().get(userSessionKey, hashKey);
        UserSession userSession = JSONObject.parseObject(o.toString(), UserSession.class);
        return userSession;

    }
}
