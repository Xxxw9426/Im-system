package com.lld.im.service.config;

import com.lld.im.common.config.AppConfig;
import com.lld.im.common.enums.ImUrlRouteWayEnum;
import com.lld.im.common.enums.RouteHashMethodEnum;
import com.lld.im.common.route.RouteHandle;
import com.lld.im.common.route.algorithm.consistenthash.AbstractConsistentHash;
import com.lld.im.common.route.algorithm.consistenthash.ConsistentHashHandle;
import com.lld.im.common.route.algorithm.consistenthash.TreeMapConsistentHash;
import com.lld.im.common.route.algorithm.loop.LoopHandle;
import com.lld.im.common.route.algorithm.random.RandomHandle;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-26
 * @Description: 将user模块中会用的一些实体类加入本模块的ioc容器
 * @Version: 1.0
 */
@Configuration
public class BeanConfig {

    @Autowired
    AppConfig appConfig;

    // 将负载均衡实现类添加到ioc容器中
    @Bean
    public RouteHandle routeHandle() throws Exception {
        Integer imRouteWay = appConfig.getImRouteWay();
        String routWay="";
        ImUrlRouteWayEnum handler=ImUrlRouteWayEnum.getHandler(imRouteWay);
        routWay= handler.getClazz();
        RouteHandle routeHandle = (RouteHandle) Class.forName(routWay).newInstance();
        if(handler==ImUrlRouteWayEnum.HASH) {
            Method setHash = Class.forName(routWay).getMethod("setHash", AbstractConsistentHash.class);
            Integer consistentHashWay = appConfig.getConsistentHashWay();
            String hashWay="";
            RouteHashMethodEnum hashHandler = RouteHashMethodEnum.getHandler(consistentHashWay);
            hashWay= hashHandler.getClazz();
            AbstractConsistentHash consistentHash = (AbstractConsistentHash) Class.forName(hashWay).newInstance();
            setHash.invoke(routeHandle, consistentHash);
        }
        return routeHandle;
    }

    // 将zookeeper客户端加入ioc容器中
    @Bean
    public ZkClient buildZKClient() {
        return new ZkClient(appConfig.getZkAddr(), appConfig.getZkConnectTimeOut());
    }
}
