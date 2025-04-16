package com.lld.im.common.enums.command;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-17
 * @Description: command枚举类
 * @Version: 1.0
 */

public enum SystemCommand implements Command{

    // 通常情况下command会被设置为16进制

    /**
     * 登录 9000
     */
    LOGIN(0x2328),


    /**
     * 登录成功返回ack 9001
     */
    LOGINACK(0x2329),


    /**
     *  登出 9003
     */
    LOGOUT(0x232b),


    /**
     *  心跳 9999
     */
    PING(0x270f),


    /**
     *  下线通知 用于多端互斥 9002
     */
    MUTUALLOGIN(0x232a),

    ;

    private int command;

    SystemCommand(int command) {this.command = command;}

    public int getCommand() {return command;}
}
