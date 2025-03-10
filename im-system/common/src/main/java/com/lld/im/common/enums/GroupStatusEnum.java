package com.lld.im.common.enums;


/***
 * 群状态的枚举类
 */
public enum GroupStatusEnum {

    /**
     * 1正常 2解散 其他待定比如封禁...
     */
    NORMAL(0),

    DESTROY(1),

    ;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static GroupStatusEnum getEnum(Integer ordinal) {

        if(ordinal == null){
            return null;
        }

        for (int i = 0; i < GroupStatusEnum.values().length; i++) {
            if (GroupStatusEnum.values()[i].getCode() == ordinal) {
                return GroupStatusEnum.values()[i];
            }
        }
        return null;
    }

    private int code;

    GroupStatusEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
