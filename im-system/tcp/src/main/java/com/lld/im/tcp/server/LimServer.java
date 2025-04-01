package com.lld.im.tcp.server;

import com.lld.im.codec.MessageDecoder;
import com.lld.im.codec.MessageEncoder;
import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.tcp.handler.HeartBeatHandler;
import com.lld.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-16
 * @Description:
 * @Version: 1.0
 */

public class LimServer {

    // 日志类
    private final static Logger logger = LoggerFactory.getLogger(LimServer.class);

    // 服务端信息配置类
    BootstrapConfig.TcpConfig config;

    EventLoopGroup mainGroup;

    EventLoopGroup subGroup;

    ServerBootstrap server;

    // 构造类
    public LimServer(BootstrapConfig.TcpConfig config) {
        this.config=config;
        // 创建两个线程池
        mainGroup = new NioEventLoopGroup(config.getBossThreadSize());
        subGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        // 创建一个服务端的主程序，这个类将引导我们的服务端进行启动工作
        server=new ServerBootstrap();
        // 将两个线程池赋值给主程序，确立了线程模型
        server.group(mainGroup, subGroup)
                // 指定IO模型为NIO
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .option(ChannelOption.SO_REUSEADDR, true) // 参数表示允许重复使用本地地址和端口
                .childOption(ChannelOption.TCP_NODELAY, true) // 是否禁用Nagle算法 简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活开关2h没有数据服务端会发送心跳包
                .childHandler(new ChannelInitializer<SocketChannel>() {   // 接下来设置我们自己定义的handler
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new MessageDecoder());  // 解码器
                        ch.pipeline().addLast(new MessageEncoder());  // 编码器
                        ch.pipeline().addLast(new IdleStateHandler(
                                0, 0,
                                10));         // 设置每超过10秒钟就触发一次检测，并且如果在检测中发现触发了超时事件后，会调用下一个handler的userEventTriggered()方法
                        ch.pipeline().addLast(new HeartBeatHandler(config.getHeartBeatTime()));
                        ch.pipeline().addLast(new NettyServerHandler(config.getBrokerId()));
                    }
                });
    }

    // 启动服务端
    public void start() {
        this.server.bind(this.config.getTcpPort());
    }

}
