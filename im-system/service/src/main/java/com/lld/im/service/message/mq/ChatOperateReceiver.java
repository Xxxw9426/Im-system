package com.lld.im.service.message.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.command.MessageCommand;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.common.model.message.MessageReadContent;
import com.lld.im.common.model.message.MessageReceiveAckContent;
import com.lld.im.common.model.message.RecallMessageContent;
import com.lld.im.service.message.service.MessageSyncService;
import com.lld.im.service.message.service.P2PMessageService;
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
 * @CreateTime: 2025-04-01
 * @Description:  订阅来自IM服务投递给我们的消息
 * @Version: 1.0
 */

@Component
public class ChatOperateReceiver {

    private static Logger logger = LoggerFactory.getLogger(ChatOperateReceiver.class);


    @Autowired
    P2PMessageService p2pMessageService;


    @Autowired
    MessageSyncService messageSyncService;


    // 在springboot里面使用消息队列Rabbitmq监听队列的消息的方法
    // 使用rabbitListener注解来注明我们监听的队列
    @RabbitListener(
            bindings = @QueueBinding(
                    value=@Queue(value= Constants.RabbitConstants.Im2MessageService,durable = "true"),      // @Queue绑定我们的队列，durable:是否持久化
                    exchange = @Exchange(value=Constants.RabbitConstants.Im2MessageService,durable = "true")    // @Exchange绑定交换机，durable:是否持久化
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
            // 接下来解析我们的command进行不同的逻辑操作
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            // TODO 如果当前指令是单聊消息
            if(command.equals(MessageCommand.MSG_P2P.getCommand())) {
                // 处理单聊消息处理逻辑
                // 将传来的消息转化我我们的类来接受
                MessageContent content = jsonObject.toJavaObject(MessageContent.class);
                p2pMessageService.process(content);

            // TODO 如果当前指令是消息接收方发送过来的receiveAck
            } else if(command.equals(MessageCommand.MSG_RECEIVE_ACK.getCommand())) {
                // 当服务端收到receiveAck时，说明此时消息已经成功发送给了消息的接收者，并且收到了消息接收者的receiveAck
                // 将接收到的消息接收者发送的receiveAck转化为我们设置的类来接受其内容
                MessageReceiveAckContent ackContent = jsonObject.toJavaObject(MessageReceiveAckContent.class);
                messageSyncService.receiveMark(ackContent);

            // TODO 如果当前指令是消息已读
            } else if(command.equals(MessageCommand.MSG_READ.getCommand())){
                MessageReadContent readContent = jsonObject.toJavaObject(MessageReadContent.class);
                messageSyncService.readMark(readContent);

            // TODO 如果当前指令是撤回消息
            } else if(Objects.equals(command, MessageCommand.MSG_RECALL.getCommand())) {
                RecallMessageContent messageContent = JSON.parseObject(msg, new TypeReference<RecallMessageContent>() {
                }.getType());
                messageSyncService.recallMessage(messageContent);
            }

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
