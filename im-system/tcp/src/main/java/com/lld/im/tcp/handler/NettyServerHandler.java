package com.lld.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lld.im.codec.pack.LoginPack;
import com.lld.im.codec.pack.group.GroupMessagePack;
import com.lld.im.codec.pack.message.ChatMessageAck;
import com.lld.im.codec.pack.user.LoginAckPack;
import com.lld.im.codec.pack.user.UserStatusChangeNotifyPack;
import com.lld.im.codec.proto.Message;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.enums.command.SystemCommand;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.ImConnectStatusEnum;
import com.lld.im.common.enums.command.UserEventCommand;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.common.model.UserSession;
import com.lld.im.common.model.message.CheckSendMessageReq;
import com.lld.im.tcp.feign.FeignMessageService;
import com.lld.im.tcp.publish.MqMessageProducer;
import com.lld.im.tcp.redis.RedisManager;
import com.lld.im.tcp.utils.SessionSocketHolder;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
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


    private FeignMessageService feignMessageService;

    // 服务层的地址
    private String logicUrl;


    public NettyServerHandler(Integer brokerId,String logicUrl) {
        this.brokerId = brokerId;
        feignMessageService= Feign.builder()
                .encoder(new JacksonEncoder())     // 编码器
                .decoder(new JacksonDecoder())     // 解码器
                .options(new Request.Options(1000,3500))   // 设置超时时间
                .target(FeignMessageService.class,logicUrl);    // 代理对象

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
            userSession.setImei(message.getMessageHeader().getImei());
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

            // 登录成功后，向service层发送消息通知用户在线状态发生变化
            UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
            userStatusChangeNotifyPack.setUserId(loginPack.getUserId());
            userStatusChangeNotifyPack.setAppId(message.getMessageHeader().getAppId());
            userStatusChangeNotifyPack.setStatus(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            // 发送到service的MQ
            MqMessageProducer.sendMessage(userStatusChangeNotifyPack,message.getMessageHeader(),
                    UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

            // 向客户端发送ack响应消息，告诉客户端它已经登录成功
            MessagePack<LoginAckPack> loginSuccess=new MessagePack<>();
            LoginAckPack loginAckPack = new LoginAckPack();
            loginAckPack.setUserId(loginPack.getUserId());
            loginSuccess.setCommand(SystemCommand.LOGINACK.getCommand());
            loginSuccess.setClientType(message.getMessageHeader().getClientType());
            loginSuccess.setData(loginAckPack);
            loginSuccess.setImei(message.getMessageHeader().getImei());
            loginSuccess.setAppId(message.getMessageHeader().getAppId());
            ctx.channel().writeAndFlush(loginSuccess);

            // TODO 如果当前指令是登出
        } else if(command== SystemCommand.LOGOUT.getCommand()) {

            SessionSocketHolder.removeUserSession((NioSocketChannel) ctx.channel());

        // TODO 如果当前指令是心跳
        // 给当前的channel绑定最后一次读写的时间
        } else if(command== SystemCommand.PING.getCommand()) {
            // 将最后一次读写事件的时间设置为当前时间
            ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());

        // TODO 如果当前指令是单聊消息或者群聊消息
        }else if(command== MessageCommand.MSG_P2P.getCommand() || command== GroupEventCommand.MSG_GROUP.getCommand()) {
            try {
                String toId="";
                CheckSendMessageReq req = new CheckSendMessageReq();
                req.setAppId(message.getMessageHeader().getAppId());
                req.setCommand(command);
                JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()));
                String fromId = jsonObject.getString("fromId");
                if(command== MessageCommand.MSG_P2P.getCommand()) {
                    toId = jsonObject.getString("toId");
                } else {
                    toId = jsonObject.getString("groupId");
                }
                req.setFromId(fromId);
                req.setToId(toId);
                // 1. 调用校验消息发送方的接口
                ResponseVO responseVO = feignMessageService.checkSendMessage(req);
                // 2. 如果成功投递给业务逻辑的MQ
                if(responseVO.isOk()) {
                    MqMessageProducer.sendMessage(message,command);
                } else {
                    Integer ackCommand=0;
                    if(command== MessageCommand.MSG_P2P.getCommand()){
                        ackCommand=MessageCommand.MSG_ACK.getCommand();
                    } else {
                        ackCommand=GroupEventCommand.GROUP_MSG_ACK.getCommand();
                    }
                    // 3. 失败则直接ack
                    ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                    MessagePack<ResponseVO> ack=new MessagePack<>();
                    responseVO.setData(chatMessageAck);
                    ack.setData(responseVO);
                    ack.setCommand(ackCommand);
                    ctx.channel().writeAndFlush(ack);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // TODO 其他消息或者请求
        } else{
            // 调用向逻辑层发送消息的方法发送给逻辑层
            MqMessageProducer.sendMessage(message,command);
        }
    }


    //表示 channel 处于不活动状态，当channel处于不活跃状态的时候会调用这个方法
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //设置离线
        SessionSocketHolder.offlineUserSession((NioSocketChannel) ctx.channel());
        ctx.close();
    }


    // 当心跳检测超时后就会进入这个方法，因此我们要在这个方法中处理心跳超时逻辑
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    }

}
