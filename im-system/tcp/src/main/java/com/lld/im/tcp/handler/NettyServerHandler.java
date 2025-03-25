package com.lld.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lld.im.codec.pack.LoginPack;
import com.lld.im.codec.proto.Message;
import com.lld.im.common.command.SystemCommand;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.common.model.UserSession;
import com.lld.im.tcp.redis.RedisManager;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description:
 * @Version: 1.0
 */

public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private final static Logger logger= LoggerFactory.getLogger(NettyServerHandler.class);

    // 当前服务端id
    private static Integer brokerId;


    public NettyServerHandler(Integer brokerId) {
        this.brokerId = brokerId;
    }

    // 有读写事件发生的时候触发
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {

        Integer command = message.getMessageHeader().getCommand();
        // 判断当前指令
        // TODO 如果当前指令是登录
        if(command== SystemCommand.LOGIN.getCommand()) {
            // 我们在私有协议中定义前端传过来的数据是json字符串
            // 因此我们对前端传过来的请求消息的消息体进行解析
            // 将前端传过来的内容存储到我们定义的用来存储前后端传输的数据包的实体类中
            // 将用户的登录信息存储到对应的实体类中
            LoginPack loginPack=JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()),
                    new TypeReference<LoginPack>(){
                    }.getType());

            // 将传输过来的数据作为属性设置给channel
            // 为channel设置用户id的属性和appId的属性以及当前用户对应的端类型的属性
            ctx.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(message.getMessageHeader().getAppId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(message.getMessageHeader().getClientType());
            ctx.channel().attr(AttributeKey.valueOf(Constants.Imei)).set(message.getMessageHeader().getImei());

            // 将用户信息存入用户session实体类对象并存入redis
            UserSession userSession=new UserSession();
            userSession.setUserId(loginPack.getUserId());
            userSession.setAppId(message.getMessageHeader().getAppId());
            userSession.setClientType(message.getMessageHeader().getClientType());
            userSession.setVersion(message.getMessageHeader().getVersion());
            userSession.setConnectState(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setBrokerId(brokerId);
            try{
                InetAddress localHost = InetAddress.getLocalHost();
                userSession.setBrokerHost(localHost.getHostAddress());
            }catch(Exception e) {
                e.printStackTrace();
            }

            // 获取RedissonClient将当前对象存到redis中
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            // 键为我们设定好的键值
            // 哈希值为当前登录用户的端类型，值为我们设定的用户Session类
            RMap<String, String> map = redissonClient.getMap(message.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(message.getMessageHeader().getClientType()+":"+message.getMessageHeader().getImei(),JSONObject.toJSONString(userSession));

            // 将channel存起来
            SessionSocketHolder.put(message.getMessageHeader().getAppId(),
                    loginPack.getUserId(),
                    message.getMessageHeader().getClientType(),
                    message.getMessageHeader().getImei(),
                    (NioSocketChannel)ctx.channel());

            // 当前用户登录成功后，向其他服务端发送消息
            // 设置要发送给其它端的数据对象
            UserClientDto dto = new UserClientDto();
            dto.setImei(message.getMessageHeader().getImei());
            dto.setUserId(loginPack.getUserId());
            dto.setClientType(message.getMessageHeader().getClientType());
            dto.setAppId(message.getMessageHeader().getAppId());
            // 使用Redis的发布订阅模式，发送给所有的服务端
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSONObject.toJSONString(dto));

            // TODO 如果当前指令是登出
        } else if(command== SystemCommand.LOGOUT.getCommand()) {

            SessionSocketHolder.removeUserSession((NioSocketChannel) ctx.channel());

        // TODO 如果当前指令是心跳
        // 给当前的channel绑定最后一次读写的时间
        } else if(command== SystemCommand.PING.getCommand()) {
            // 将最后一次读写事件的时间设置为当前时间
            ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());
        }
    }


    // 当心跳检测超时后就会进入这个方法，因此我们要在这个方法中处理心跳超时逻辑
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    }

}
