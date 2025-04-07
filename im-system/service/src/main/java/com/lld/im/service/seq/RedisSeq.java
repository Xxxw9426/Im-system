package com.lld.im.service.seq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-05
 * @Description: 通过基于redis的序列号实现消息的有序性
 * @Version: 1.0
 */
@Service
public class RedisSeq {


    @Autowired
    StringRedisTemplate stringRedisTemplate;


    /***
     *  根据传入的key值返回一个绝对递增后的key
     * @param key
     * @return
     */
    public Long doGetSeq(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

}
