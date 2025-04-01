package com.lld.im.tcp.receiver.process;

import com.lld.im.codec.proto.MessagePack;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-29
 * @Description: 处理监听到的消息的工厂类实例
 * @Version: 1.0
 */

public abstract class BaseProcess {


    // 处理前的方法
    public abstract void processBefore();

    /***
     * 将监听到的消息处理并投递给对应用户
     * @param messagePack
     */
    public void process(MessagePack messagePack) {
        processBefore();
        // 根据messagePack中的信息获取到要投递给的用户的channel
        NioSocketChannel channel = SessionSocketHolder.get(messagePack.getAppId(), messagePack.getToId(),
                messagePack.getImei(), messagePack.getClientType());
        if(channel != null) {
            channel.writeAndFlush(messagePack);
        }
        processAfter();
    }


    // 处理后的方法
    public abstract void processAfter();


}
