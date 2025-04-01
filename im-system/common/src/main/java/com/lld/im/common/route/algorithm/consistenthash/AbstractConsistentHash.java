package com.lld.im.common.route.algorithm.consistenthash;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-26
 * @Description: 定义一个抽象类来作为项目中一致性hash实现方式的拓展
 * @Version: 1.0
 */

public abstract class AbstractConsistentHash {

    /***
     * 给hash环中添加元素(添加服务端连接地址节点)的方法
     * @param key
     * @param value
     */
    protected abstract void add(long key,String value);


    /***
     *  hash环进行排序(不是一定要子类重写，因为有的数据类型自带了排序方法)
     */
    protected void sort() {}


    /***
     *  获取hash环中的节点的方法
     * @param value  value指的是用户的userId
     */
    protected abstract String getFirstNodeValue(String value);


    /***
     *  hash算法，hash算法支持自己定义
     * @param value   value指的是用户的userId
     * @return
     */
    public Long hash(String value){
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + value, e);
        }

        md5.update(keyBytes);
        byte[] digest = md5.digest();

        // hash code, Truncate to 32-bits
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }


    /**
     * 正式处理之前前置数据处理
     */
    protected abstract void processBefore();


    /**
     * 传入节点列表以及客户端信息获取一个服务节点
     * @param values
     * @param key
     * @return
     */
    public synchronized String process(List<String> values, String key){
        processBefore();
        for (String value : values) {
            add(hash(value), value);
        }
        sort();
        return getFirstNodeValue(key) ;
    }
}
