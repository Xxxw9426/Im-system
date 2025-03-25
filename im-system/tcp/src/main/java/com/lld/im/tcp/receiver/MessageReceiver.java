package com.lld.im.tcp.receiver;

import com.lld.im.common.constant.Constants;
import com.lld.im.tcp.utils.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-23
 * @Description: 用来监听逻辑层投递过来的消息的组件工具类
 * @Version: 1.0
 */

@Slf4j
public class MessageReceiver {

    private static String brokerId;

    private static void startReceiverMessage() {

        try {
            // 获取监听消息服务给IM投递的消息的channel
            Channel channel = MqFactory.getChannel(Constants.RabbitConstants.MessageService2Im+brokerId);
            // 给channel绑定监听的消息队列
            /** 这个方法的参数：
             *       1.队列名称
             *       2.队列是否持久化(true/false)
             *             true:队列将被持久化到磁盘。即使 RabbitMQ 服务器重启，队列也会被保留。
             *             false:队列是临时的，仅存在于当前服务器运行期间。服务器重启后，队列将丢失。
             *       3.队列是否排他(true/false)
             *             true:队列是排他的，只能被声明它的连接（Connection）或通道（Channel）使用。
             *                  其他连接或通道无法访问该队列。一旦声明它的连接关闭，队列将被自动删除。
             *             false:队列不是排他的，任何连接或通道都可以访问该队列。
             *       4.队列是否自动删除(true/false)
             *             true:队列在最后一个消费者断开连接后将被自动删除。这适用于临时队列，例如在某些消息消费场景中，队列仅在有消费者时存在。
             *             false:队列不会自动删除，需要手动删除。
             *       5.队列的额外参数（如消息过期时间、队列最大长度等）
             */
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im+brokerId,
                    true, false,false,null
                    );
            // 给channel绑定交换机
            /** 这个方法的参数：
             *       1.队列名称
             *       2.交换机名称
             *       3.路由routingKey
             */
            channel.queueBind(Constants.RabbitConstants.MessageService2Im+brokerId,Constants.RabbitConstants.MessageService2Im,brokerId);
            // 开始监听
            /** 这个方法的参数：
             *       1.要监听的队列名称
             *       2.是否自动确认(true/false)
             *             true:自动确认。当消费者接收到消息后，RabbitMQ 会自动发送确认信号（ACK），表示消息已被成功处理。
             *                  这种方式简单，但可能会导致消息丢失，因为如果消费者在处理消息时失败（例如抛出异常），RabbitMQ 会认为消息已经被处理。
             *             false:手动确认。消费者需要在处理完消息后显式调用 basicAck 方法来确认消息。
             *                  这种方式更安全，因为消费者可以在处理成功后才确认消息，如果处理失败，RabbitMQ 会重新发送消息。
             *       3.消息回调函数 ：定义了如何处理接收到的消息。当队列中有消息到达时，RabbitMQ 会调用这个回调方法来处理消息。
             */
            channel.basicConsume(Constants.RabbitConstants.MessageService2Im+brokerId,false,new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    // 当监听到消息后的消息处理逻辑
                    String message = new String(body);
                    log.info(message);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // 对外暴露一个启动该监听消息类的初始化方法
    public static void init() {
        startReceiverMessage();
    }

    // 对外暴露一个可以通过掺入brokerId启动该监听消息类的初始化方法
    public static void init(String brokerId) {
        if(StringUtils.isBlank(MessageReceiver.brokerId)) {
            MessageReceiver.brokerId = brokerId;
        }
        startReceiverMessage();
    }


}
