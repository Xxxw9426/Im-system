package com.lld.im.service.utils;

import com.lld.im.common.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-07
 * @Description: 生成seq的工具类
 * @Version: 1.0
 */

@Service
public class WriteUserSeq {

    // 使用redis中的hash
    // userId  friend    10
    //         group     20
    //         conversation     123

    @Autowired
    RedisTemplate redisTemplate;


    /***
     * 当我们IM服务中某个用户的不同模块的数据发生变化后根据传入的值将其最新的seq存入内存
     * @param appId
     * @param userId
     * @param type
     * @param seq
     */
    public void writeUserSeq(Integer appId,String userId,String type,Long seq) {
        String key=appId+":"+ Constants.RedisConstants.SeqPrefix +":"+userId;
        redisTemplate.opsForHash().put(key,type,seq);

    }
}
