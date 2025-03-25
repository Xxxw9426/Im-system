package com.lld.im.tcp.receiver;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.MessagePack;
import com.lld.im.common.ClientType;
import com.lld.im.common.command.Command;
import com.lld.im.common.command.SystemCommand;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.DeviceMultiLoginEnum;
import com.lld.im.common.model.UserClientDto;
import com.lld.im.tcp.redis.RedisManager;
import com.lld.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-25
 * @Description: 监听并处理用户登录消息的类
 *  多端登录多端同步： 1 单端登录  只允许一端在线：踢掉除了本clientType+imei的设备
 *                    2 双端登录  允许pc/mobile其中一端登录+web端登录：踢掉除了本clientType+imei以及web端以外的设备
 *                    3 三端登录  允许mobile+pc+web三端同时登录：踢除同clientType的其他imei，除了web
 *                    4 多端登录  不做任何处理
 * @Version: 1.0
 */

public class UserLoginMessageListener {

    private final static Logger logger = LoggerFactory.getLogger(UserLoginMessageListener.class);

    // 记录多端登录多端同步的模式
    private Integer loginModel;

    public UserLoginMessageListener(Integer loginModel) {
        this.loginModel = loginModel;
    }

    // 监听用户登录的方法
    public void listenerUserLogin() {
        // 拿到用户登录消息的topic，标识监听这个topic
        RTopic topic = RedisManager.getRedissonClient().getTopic(Constants.RedisConstants.UserLoginChannel);
        // 为这个topic设置监听事件
        topic.addListener(String.class, new MessageListener<String>() {
            // 监听到用户的登录消息后的处理
            @Override
            public void onMessage(CharSequence charSequence, String msg) {
                logger.info("收到用户上线通知：" + msg);
                // 将接收到的消息转化成对象
                UserClientDto dto = JSONObject.parseObject(msg, UserClientDto.class);
                // 获取本台服务器中所有这个用户的channel
                List<NioSocketChannel> channels = SessionSocketHolder.get(dto.getAppId(), dto.getUserId());
                // 遍历进行处理
                for (NioSocketChannel channel : channels) {
                    // 判断当前的登录模式
                    if(loginModel== DeviceMultiLoginEnum.ONE.getLoginMode()) {    // TODO 单端
                        // 首先获取当前遍历到的用户的标识：clientType+Imei
                        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                        // 与当前正在登录用户的唯一标识：clientType+Imei 进行比较
                        if(!(clientType + ":" + imei).equals(dto.getClientType()+":"+dto.getImei())){
                            // TODO 如果不相等的话，需要踢掉我们遍历到的客户端
                            // 告诉客户端，其它端登录，当前端被强制下线，由客户端选择要重新登录还是退出登录
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            pack.setToId((String)channel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String)channel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            channel.writeAndFlush(pack);
                        }

                    } else if(loginModel== DeviceMultiLoginEnum.TWO.getLoginMode()) {      // TODO 双端
                        // 如果当前正在登陆的用户是web端，不做处理，web端支持多端登录
                        if(dto.getClientType() == ClientType.WEB.getCode()){
                            continue;
                        }
                        // 再判断当前遍历到的端是否是web端
                        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                        // 如果是的话也继续执行，不做处理
                        if (clientType == ClientType.WEB.getCode()){
                            continue;
                        }
                        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                        // 与当前正在登录用户的唯一标识：clientType+Imei 进行比较
                        if(!(clientType + ":" + imei).equals(dto.getClientType()+":"+dto.getImei())){
                            // TODO 如果不相等的话，需要踢掉我们遍历到的客户端
                            // 告诉客户端，其它端登录，当前端被强制下线，由客户端选择要重新登录还是退出登录
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setToId((String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            channel.writeAndFlush(pack);
                        }
                    } else if(loginModel== DeviceMultiLoginEnum.THREE.getLoginMode()) {    // TODO 三端
                        // 如果当前正在登陆的用户是web端，不做处理，web端支持多端登录
                        if(dto.getClientType() == ClientType.WEB.getCode()){
                            continue;
                        }
                        // 首先获取当前遍历到的用户的标识：clientType+Imei
                        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                        // 判断当前登录是否是同端
                        boolean isSameClient=false;
                        // 如果当前正在遍历的端是手机端(ios/android)并且正在登录的端也是手机端(ios/android)
                        if( (clientType==ClientType.IOS.getCode() || clientType==ClientType.ANDROID.getCode())
                                && (dto.getClientType()==ClientType.IOS.getCode() || dto.getClientType()==ClientType.ANDROID.getCode())
                         ) {
                            isSameClient=true;
                        }
                        // 如果当前正在遍历的端是电脑端(mac/windows)并且正在登录的端也是电脑端(mac/windows)
                        if( (clientType==ClientType.MAC.getCode() || clientType==ClientType.WINDOWS.getCode())
                                && (dto.getClientType()==ClientType.MAC.getCode() || dto.getClientType()==ClientType.WINDOWS.getCode())
                        ) {
                            isSameClient=true;
                        }
                        // 如果是相同端并且我们的唯一标识不同，则需要踢除我们遍历到的端
                        if(isSameClient && !(clientType + ":" + imei).equals(dto.getClientType()+":"+dto.getImei())) {
                            // TODO 如果不相等的话，需要踢掉我们遍历到的客户端
                            // 告诉客户端，其它端登录，当前端被强制下线，由客户端选择要重新登录还是退出登录
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setToId((String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            channel.writeAndFlush(pack);
                        }
                    }
                }

            }
        });
    }

}
