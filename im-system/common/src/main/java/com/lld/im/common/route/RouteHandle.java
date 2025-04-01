package com.lld.im.common.route;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-26
 * @Description: 定义三种负载均衡模式的接口
 * @Version: 1.0
 */

public interface RouteHandle {


    /***
     * 向sdk返回一个可用的server服务器地址的方法
     * @param servers
     * @param key
     * @return
     */
    public String routeServer(List<String> servers,String key);
}
