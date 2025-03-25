package com.lld.im.tcp.handler;

import com.lld.im.common.constant.Constants;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-19
 * @Description: 心跳检测超时处理类
 * @Version: 1.0
 */

@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    // 心跳检测超时时间
    private Long heartBeatTime;

    public HeartBeatHandler(Long heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }

    // 当心跳检测超时后就会进入这个方法，因此我们要在这个方法中处理心跳超时逻辑
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断evt是否是IdleStateEvent（用于触发用户事件，包含 读空闲/写空闲/读写空闲 ）
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent)evt;		// 强制类型转换
            if (event.state() == IdleState.READER_IDLE) {
                log.info("读空闲");
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.info("进入写空闲");
            } else if (event.state() == IdleState.ALL_IDLE) {

                // 获取当前channel上一次读写操作的时间
                Long lastReadTime = (Long) ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).get();
                // 获取当前时间
                long currentTime = System.currentTimeMillis();
                // 如果现在时间距离上次读写时间超过了心跳时间
                if(lastReadTime!=null && currentTime - lastReadTime > heartBeatTime){
                    // 执行离线逻辑 : 修改用户的状态为离线状态
                    SessionSocketHolder.offlineUserSession((NioSocketChannel) ctx.channel());
                }

            }
        }
    }
}
