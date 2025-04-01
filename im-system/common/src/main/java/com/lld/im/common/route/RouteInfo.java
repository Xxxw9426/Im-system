package com.lld.im.common.route;

import lombok.Data;

/**
 * 服务端连接地址信息类
 * @since JDK 1.8
 */

@Data
public final class RouteInfo {

    private String ip;
    private Integer port;

    public RouteInfo(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }
}
