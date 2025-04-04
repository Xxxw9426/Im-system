package com.lld.im.common.enums;

/***
 * 具体的一致性hash算法的实现的枚举类
 */
public enum RouteHashMethodEnum {

    /**
     * TreeMap
     */
    TREE(1,"com.lld.im.common.route.algorithm.consistenthash" +
            ".TreeMapConsistentHash"),

    /**
     * 自定义map
     */
    CUSTOMER(2,"com.lld.im.common.route.algorithm.consistenthash.xxxx"),

    ;


    private int code;
    private String clazz;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static RouteHashMethodEnum getHandler(int ordinal) {
        for (int i = 0; i < RouteHashMethodEnum.values().length; i++) {
            if (RouteHashMethodEnum.values()[i].getCode() == ordinal) {
                return RouteHashMethodEnum.values()[i];
            }
        }
        return null;
    }

    RouteHashMethodEnum(int code, String clazz){
        this.code=code;
        this.clazz=clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public int getCode() {
        return code;
    }
}
