package com.lld.im.tcp;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.codec.proto.Message;
import com.lld.im.tcp.receiver.MessageReceiver;
import com.lld.im.tcp.redis.RedisManager;
import com.lld.im.tcp.register.RegistryZK;
import com.lld.im.tcp.register.ZKit;
import com.lld.im.tcp.server.LimServer;
import com.lld.im.tcp.server.LimWebSocketServer;

import com.lld.im.tcp.utils.MqFactory;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-16
 * @Description: 网关的启动类
 * @Version: 1.0
 */

public class Starter {
    public static void main(String[] args) {
        // arg[0]中配置的是我们配置文件的路径
        if(args.length > 0) {
            start(args[0]);
        }
    }

    private static void start(String path) {

        try {
            Yaml yaml = new Yaml();
            InputStream is=new FileInputStream(path);
            // 根据配置好的路径加载BootstrapConfig类，该类中为配置文件的信息
            BootstrapConfig config = yaml.loadAs(is, BootstrapConfig.class);

            // 启动我们的Netty NIO服务端
            new LimServer(config.getLim()).start();
            // 启动我们的webSocket服务端
            new LimWebSocketServer(config.getLim()).start();
            // 启动我们的Redis的客户端RedissonClient
            RedisManager.init(config);
            // 启动我们的RabbitMQ
            MqFactory.init(config.getLim().getRabbitmq());
            // 启动监听逻辑层投递过来的消息的组件工具类
            MessageReceiver.init(config.getLim().getBrokerId()+"");
            // 注册zookeeper
            registerZK(config);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }

    }

    // 注册zookeeper的方法
    public static void registerZK(BootstrapConfig config) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        // 首先根据配置文件中的zookeeper的地址和超时时间创建ZKClient对象
        ZkClient zkClient = new ZkClient(config.getLim().getZkConfig().getZkAddr(), config.getLim().getZkConfig().getZkConnectTimeOut());
        // 根据zkClient对象创建ZKit对象
        ZKit zKit = new ZKit(zkClient);
        // 根据zKit创建RegisterZK对象
        RegistryZK registryZK = new RegistryZK(zKit, hostAddress, config.getLim());
        Thread thread = new Thread(registryZK);
        thread.start();
    }
}
