package com.lld.im.common.enums;

/***
 * 用户禁用状态枚举类
 */
public enum UserForbiddenFlagEnum {

    /**
     * 0 正常；1 禁用。
     */
    NORMAL(0),

    FORBIBBEN(1),
    ;

    private int code;

    UserForbiddenFlagEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
