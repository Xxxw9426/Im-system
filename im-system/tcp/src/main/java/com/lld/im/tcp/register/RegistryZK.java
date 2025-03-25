package com.lld.im.tcp.register;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.common.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-24
 * @Description: 用来在zookeeper中注册服务的类
 * @Version: 1.0
 */

public class RegistryZK implements Runnable{

    private static Logger logger = LoggerFactory.getLogger(RegistryZK.class);

    private ZKit zKit;

    private String ip;

    private BootstrapConfig.TcpConfig tcpConfig;

    public RegistryZK(ZKit zKit, String ip, BootstrapConfig.TcpConfig tcpConfig) {
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    // 在run方法中初始化zookeeper
    @Override
    public void run() {

        // 实现调用方法创建父节点
        zKit.createRootNode();
        // 首先创建tcp
        // 获取我们的完整路径，为后续做准备
        String tcpPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        // 根据这个path生成我们的子节点
        zKit.createNode(tcpPath);
        // 打印日志：tcp服务启动成功
        logger.info("Registry zookeeper tcpPath success, msg=[{}]", tcpPath);

        // 接下来创建webSocket
        // 获取我们的完整路径，为后续做准备
        String webPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootWeb+ "/" + ip + ":" + tcpConfig.getWebSocketPort();
        // 根据这个path生成我们的子节点
        zKit.createNode(webPath);
        // 打印日志：tcp服务启动成功
        logger.info("Registry zookeeper webPath success, msg=[{}]", webPath);

    }
}
