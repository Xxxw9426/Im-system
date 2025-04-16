package com.lld.im.service.user.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.command.UserEventCommand;
import com.lld.im.service.message.mq.ChatOperateReceiver;
import com.lld.im.service.user.model.UserStatusChangeNotifyContent;
import com.lld.im.service.user.service.ImUserService;
import com.lld.im.service.user.service.ImUserStatusService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-10
 * @Description: 用户在线状态修改消息接收类
 * @Version: 1.0
 */

@Component
public class UserOnlineStatusReceiver {

    private static Logger logger = LoggerFactory.getLogger(UserOnlineStatusReceiver.class);

    @Autowired
    ImUserStatusService imUserStatusService;


    // 在springboot里面使用消息队列Rabbitmq监听队列的消息的方法
    // 使用rabbitListener注解来注明我们监听的队列
    @RabbitListener(
            bindings = @QueueBinding(
                    value=@Queue(value= Constants.RabbitConstants.Im2UserService,durable = "true"),      // @Queue绑定我们的队列，durable:是否持久化
                    exchange = @Exchange(value=Constants.RabbitConstants.Im2UserService,durable = "true")    // @Exchange绑定交换机，durable:是否持久化
            ),concurrency = "1"                                      // concurrency:每一次向我们的队列中拉取多少条消息

    )
    public void onChatMessage(@Payload Message message , @Headers Map<String,Object> headers,
                              Channel channel) throws Exception{

        long start = System.currentTimeMillis();
        Thread t = Thread.currentThread();
        String msg = new String(message.getBody(), "utf-8");
        logger.info("CHAT MSG FROM QUEUE :::::" + msg);
        //deliveryTag 用于回传 rabbitmq 确认该消息处理成功
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try{

            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            // TODO 如果当前command是用户登录状态发生变化
            if(Objects.equals(command, UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand())){
                // 将传过来的消息转化为我们会用到的对象
                UserStatusChangeNotifyContent content = JSON.parseObject(msg, new TypeReference<UserStatusChangeNotifyContent>() {
                }.getType());
                // 调用处理用户在线状态发生变化的方法
                imUserStatusService.processUserOnlineStatusNotify(content);
            }

            channel.basicAck(deliveryTag,false);

        } catch(Exception e) {
            logger.error("处理消息出现异常：{}",e.getMessage());
            logger.error("RMQ_CHAT_TRAN_ERROR", e);
            logger.error("NACK_MSG:{}", msg);
            //第一个false 表示不批量拒绝，第二个false表示不重回队列
            channel.basicNack(deliveryTag, false, false);
        } finally {
            long end = System.currentTimeMillis();
            logger.debug("channel {} basic-Ack ,it costs {} ms,threadName = {},threadId={}", channel, end - start, t.getName(), t.getId());
        }


    }
}
