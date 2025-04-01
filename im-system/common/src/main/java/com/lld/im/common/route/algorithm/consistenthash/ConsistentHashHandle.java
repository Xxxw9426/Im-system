package com.lld.im.common.route.algorithm.consistenthash;

import com.lld.im.common.route.RouteHandle;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-26
 * @Description: 采用一次性hash的方式实现负载均衡的实现类
 * @Version: 1.0
 */

public class ConsistentHashHandle implements RouteHandle {

    // TreeMap
    private AbstractConsistentHash hash;


    public void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }

    /***
     *  从传入的server列表中根据用户的userId的hash值取出对应的server服务器地址返回给sdk
     *  实现思路：
     *      一致性hash其实有很多的实现思路，在这里我们使用抽象类定义了一致性hash需要实现的方法
     *      然后又通过treemap实现了这个抽象类
     * @param servers
     * @param key
     * @return
     */
    @Override
    public String routeServer(List<String> servers, String key) {
        return hash.process(servers, key);
    }
}
