package com.lld.im.tcp.register;

import com.lld.im.common.constant.Constants;
import org.I0Itec.zkclient.ZkClient;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-24
 * @Description: 工具类,用来前置处理一些zookeeper相关的设置
 * @Version: 1.0
 */

public class ZKit {

    private ZkClient zkClient;


    public ZKit(ZkClient zkClient) {
        this.zkClient = zkClient;
    }


    // TODO 我们要通过ZkClient来初始化我们的节点，对于节点的存放方式我们的设计如下：//im-coreRoot/tcp(web)/ip:port
    // 创建父节点(//im-coreRoot/tcp(web))的方法
    public void createRootNode() {

        // 首先判断父节点(//im-coreRoot)是否存在
        boolean exists = zkClient.exists(Constants.ImCoreZkRoot);
        if(!exists) {
            // 如果父节点不存在的话创建临时节点
            zkClient.createPersistent(Constants.ImCoreZkRoot);
        }

        // 创建下一层的节点(/tcp)
        boolean tcpExists = zkClient.exists(Constants.ImCoreZkRoot+Constants.ImCoreZkRootTcp);
        if(!tcpExists) {
            // 如果下一层节点不存在的话创建临时节点
            zkClient.createPersistent(Constants.ImCoreZkRoot+Constants.ImCoreZkRootTcp);
        }

        // 继续创建下一层的节点(/web)
        boolean webExists = zkClient.exists(Constants.ImCoreZkRoot+Constants.ImCoreZkRootWeb);
        if(!webExists) {
            // 如果下一层节点不存在的话创建临时节点
            zkClient.createPersistent(Constants.ImCoreZkRoot+Constants.ImCoreZkRootWeb);
        }
    }

    // 创建子节点(/ip:port)的方法
    public void createNode(String path) {
        // 判断传入的path是否存在
        // 如果不存在的话要创建
        if(!zkClient.exists(path)) {
            zkClient.createPersistent(path);
        }
    }


}
