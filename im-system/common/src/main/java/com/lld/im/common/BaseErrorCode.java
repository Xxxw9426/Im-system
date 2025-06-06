package com.lld.im.common;


import com.lld.im.common.exception.ApplicationExceptionEnum;

/**
 * TODO 该方法的标识是个E，表示该方法是枚举类，该类是公共的返回错误码
 */
public enum BaseErrorCode implements ApplicationExceptionEnum {

    SUCCESS(200,"success"),
    SYSTEM_ERROR(90000,"服务器内部错误,请联系管理员"),
    PARAMETER_ERROR(90001,"参数校验错误"),


            ;

    private int code;
    private String error;

    BaseErrorCode(int code, String error){
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
