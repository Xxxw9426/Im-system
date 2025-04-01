package com.lld.im.common.route.algorithm.random;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.route.RouteHandle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-26
 * @Description: 采用随机方式实现负载均衡的实现类
 * @Version: 1.0
 */

public class RandomHandle implements RouteHandle {

    /***
     * 在传入的server列表中随机取一个可用的server服务器地址返回给sdk
     * @param servers
     * @param key
     * @return
     */
    @Override
    public String routeServer(List<String> servers, String key) {
        int size = servers.size();
        // 判断当前所有服务的数量，如果数量为0则抛出异常：没有可用的服务
        if(size == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        // 通过size生成一个随机数，再通过随机数在list中获取一个server返回
        int i = ThreadLocalRandom.current().nextInt(size);
        return servers.get(i);
    }
}
