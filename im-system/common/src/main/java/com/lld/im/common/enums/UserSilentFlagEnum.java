package com.lld.im.common.enums;

/***
 * 用户禁言状态枚举类
 */
public enum UserSilentFlagEnum {

    /**
     * 0 正常；1 禁言。
     */
    NORMAL(0),

    MUTE(1),
    ;

    private int code;

    UserSilentFlagEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
