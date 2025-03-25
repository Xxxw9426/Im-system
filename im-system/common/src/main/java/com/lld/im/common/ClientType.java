package com.lld.im.common;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-25
 * @Description: 用户登录的clientType枚举类
 * @Version: 1.0
 */

public enum ClientType {

    WEBAPI(0,"webApi"),
    WEB(1,"web"),
    IOS(2,"ios"),
    ANDROID(3,"android"),
    WINDOWS(4,"windows"),
    MAC(5,"mac"),
    ;

    private int code;
    private String error;

    ClientType(int code, String error){
        this.code = code;
        this.error = error;
    }
    public int getCode() {
        return this.code;
    }

    public String getError() {
        return this.error;
    }
}
