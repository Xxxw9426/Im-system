package com.lld.im.codec;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.proto.Message;
import com.lld.im.codec.proto.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-16
 * @Description: 自定义我们项目中的私有协议解码类
 * @Version: 1.0
 */

public class MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        /**  私有协议定义：
         *   请求头：
         *    指令 (标明我们本次的请求要做什么，即例如HTTP中的Get，Post，Delete，Put)
         *    版本
         *    端标识clientType (IOS，安卓，pc [windows，mac]，web)。
         *    消息解析类型(标识以什么形式来解析我们的请求体)。
         *    appId
         *    imei长度
         *    请求体长度bodylen
         *    +imei号
         *    + 请求体
         */

        // 如果我们消息的长度小于28则说明其不符合我们自定义协议的规范，返回
        if(in.readableBytes() < 28) {
            return;
        }

        /* 获取command指令 */
        int command = in.readInt();

        /* 获取version版本 */
        int version = in.readInt();

        /* 获取clientType端类型 */
        int clientType = in.readInt();

        /* 获取messageType消息解析类型 */
        int messageType = in.readInt();

        /* 获取appId */
        int appId = in.readInt();

        /* 获取imeiLength */
        int imeiLength = in.readInt();

        /* 获取bodyLength */
        int bodyLength = in.readInt();

        if(in.readableBytes() < bodyLength + imeiLength) {
            // 返回上一次读取后标记的下标
            in.resetReaderIndex();
            return;
        }

        // 通过imeiLength读取imei号
        byte[] imeiData = new byte[imeiLength];
        in.readBytes(imeiData);
        String imei = new String(imeiData);

        // 通过bodyLength读取body
        byte[] bodyData = new byte[bodyLength];
        in.readBytes(bodyData);

        // 将读取到的内容封装到我们设置的实体类中
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setCommand(command);
        messageHeader.setAppId(appId);
        messageHeader.setClientType(clientType);
        messageHeader.setLength(bodyLength);
        messageHeader.setImei(imei);
        messageHeader.setMessageType(messageType);
        messageHeader.setImeiLength(imeiLength);
        messageHeader.setVersion(version);

        // 设置消息类并把
        Message message = new Message();
        message.setMessageHeader(messageHeader);

        // 以0x0来表示解析格式为json格式
        if(messageType==0x0) {
            // 将我们的数据解析为json格式
            String body = new String(bodyData);
            JSONObject parse =(JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }

        // 标记本次读取后的下标
        in.markReaderIndex();
        out.add(message);
    }
}
