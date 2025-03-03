package com.lld.im.common.enums;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 好友关系实体类中字段默认值的枚举类
 * @Version: 1.0
 */

public enum FriendShipStatusEnum {

    /**
     * 好友状态：0未添加 1正常 2删除
     */
    FRIEND_STATUS_NO_FRIEND(0),

    FRIEND_STATUS_NORMAL(1),

    FRIEND_STATUS_DELETE(2),

    /**
     * 拉黑状态：0未添加 1正常 2删除
     */
    BLACK_STATUS_NORMAL(1),

    BLACK_STATUS_BLACKED(2),
            ;

    private int code;

    FriendShipStatusEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }

}
