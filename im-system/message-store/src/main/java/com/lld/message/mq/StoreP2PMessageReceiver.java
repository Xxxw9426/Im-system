package com.lld.message.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.constant.Constants;
import com.lld.message.dao.ImMessageBodyEntity;
import com.lld.message.model.DoStoreP2PMessageDto;
import com.lld.message.service.StoreMessageService;
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
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-02
 * @Description: message-store包中真正处理消息持久化的类
 * @Version: 1.0
 */
@Component
public class StoreP2PMessageReceiver {

    private static Logger logger = LoggerFactory.getLogger(StoreP2PMessageReceiver.class);


    @Autowired
    StoreMessageService storeMessageService;


    // 在springboot里面使用消息队列Rabbitmq监听队列的消息的方法
    // 使用rabbitListener注解来注明我们监听的队列
    @RabbitListener(
            bindings = @QueueBinding(
                    value=@Queue(value= Constants.RabbitConstants.StoreP2PMessage,durable = "true"),      // @Queue绑定我们的队列，durable:是否持久化
                    exchange = @Exchange(value=Constants.RabbitConstants.StoreP2PMessage,durable = "true")    // @Exchange绑定交换机，durable:是否持久化
            ),concurrency = "1"                                      // concurrency:每一次向我们的队列中拉取多少条消息

    )
    public void onChatMessage(@Payload Message message , @Headers Map<String,Object> headers,
                              Channel channel) throws Exception {
        // 处理逻辑
        // 获取我们的数据，数据是byte数组，再将其转化为String字符串
        String msg=new String(message.getBody(),"utf-8");
        // 打印我们的数据
        logger.info("CHAT MSG FORM QUEUE ::: {}", msg);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try{
            // 首先将队列中的消息由json字符串形式转化为我们设置的DoStoreP2PMessageDto
            JSONObject jsonObject = JSON.parseObject(msg);
            // 由于jsonObject中没有ImMessageBodyEntity类，所以此时转化是空，我们要对它进行赋值
            DoStoreP2PMessageDto messageDto = jsonObject.toJavaObject(DoStoreP2PMessageDto.class);
            // 获取jsonObject中的ImMessageBody
            ImMessageBodyEntity messageBody = jsonObject.getObject("imMessageBody", ImMessageBodyEntity.class);
            // 赋值
            messageDto.setImMessageBody(messageBody);
            // 持久化我们的消息
            storeMessageService.doStoreP2PMessage(messageDto);
            // 返回ack
            channel.basicAck(deliveryTag,false);

        } catch(Exception e) {
            logger.error("处理消息出现异常：{}", e.getMessage());
            logger.error("RMQ_CHAT_TRAN_ERROR", e);
            logger.error("NACK_MSG:{}", msg);
            //第一个false 表示不批量拒绝，第二个false表示不重回队列
            channel.basicNack(deliveryTag, false, false);
        }



    }
}
