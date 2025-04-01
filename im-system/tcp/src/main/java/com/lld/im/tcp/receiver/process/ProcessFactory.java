package com.lld.im.tcp.receiver.process;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-29
 * @Description: 处理监听到的消息的工厂类
 * @Version: 1.0
 */

public class ProcessFactory {

    private static BaseProcess defaultProcess;

    static{
        defaultProcess=new BaseProcess() {
            @Override
            public void processBefore() {

            }

            @Override
            public void processAfter() {

            }
        };
    }

    public static BaseProcess getMessageProcess(Integer command) {
        return defaultProcess;
    }
}
