package com.lld.im.common.enums.command;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-29
 * @Description: 用户模块发送tcp的command
 * @Version: 1.0
 */

public enum UserEventCommand implements Command{

    /**
     * 用户修改 4000
     */
    USER_MODIFY(4000),


    /**
     * 4001 用户在线状态变化(tcp服务发送给业务逻辑service层)
     */
    USER_ONLINE_STATUS_CHANGE(4001),


    /**
     * 用户在线状态通知报文 4004(业务逻辑service层发送给其他用户在线客户端)
     */
    USER_ONLINE_STATUS_CHANGE_NOTIFY(4004),


    /**
     * 用户在线状态通知同步报文 4005(业务逻辑service层发送给自身其他在线客户端)
     */
    USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC(4005),


    ;

    private int command;

    UserEventCommand(int command){
        this.command=command;
    }


    @Override
    public int getCommand() {
        return command;
    }

}
