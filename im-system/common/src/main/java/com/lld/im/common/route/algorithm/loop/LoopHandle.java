package com.lld.im.common.route.algorithm.loop;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.route.RouteHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-26
 * @Description: 采用轮询方式实现负载均衡的实现类
 * @Version: 1.0
 */

public class LoopHandle implements RouteHandle {

    private AtomicLong index=new AtomicLong();

    /***
     *  采用轮询的方式从传入的server列表中取一个可用的server服务器地址返回给sdk
     *  实现思路：
     *  每次对size进行取模运算，并且要对每次对size进行运算的数字进行自增操作
     *  这样就可以实现每次取模后的结果加一并且小于size
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
        Long l=index.incrementAndGet()%size;
        if(l<0) {
            l=0L;
        }
        return servers.get(l.intValue());
    }
}
