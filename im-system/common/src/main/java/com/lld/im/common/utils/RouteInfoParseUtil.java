package com.lld.im.common.utils;


import com.lld.im.common.BaseErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.route.RouteInfo;

/**
 * 将从zookeeper中拿到的ip:端口号的信息转化成ip+端口号
 * @since JDK 1.8
 */
public class RouteInfoParseUtil {

    public static RouteInfo parse(String info){
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo =  new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1])) ;
            return routeInfo ;
        }catch (Exception e){
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR) ;
        }
    }
}
