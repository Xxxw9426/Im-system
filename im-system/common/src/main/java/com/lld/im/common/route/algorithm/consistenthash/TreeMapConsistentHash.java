package com.lld.im.common.route.algorithm.consistenthash;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-26
 * @Description: 使用hashmap实现一致性hash的类
 * @Version: 1.0
 */

public class TreeMapConsistentHash extends AbstractConsistentHash{

    private TreeMap<Long,String> treeMap =new TreeMap<>();

    // 每个真实节点创建虚拟节点的个数
    private static final int NODE_SIZE=2;

    /***
     * 给hash环中添加元素(添加服务端连接地址节点)的方法
     * @param key
     * @param value
     */
    @Override
    protected void add(long key, String value) {

        // 循环为每个真实节点创建虚拟节点
        for(int i=0;i<NODE_SIZE;i++){
            treeMap.put(super.hash("node"+key+i),value);
        }
        treeMap.put(key,value);
    }

    /***
     * 根据userId获取hash值，然后拿着这个hash值找hash环中离这个hash值最近的节点返回其值
     * @param value  value指的是用户的userId
     * @return
     */
    @Override
    protected String getFirstNodeValue(String value) {
        Long hash = super.hash(value);
        /***
         *  tailMap(hash)
         *  这个方法用于返回此映射中键大于或等于指定键 hash的部分视图
         */
        SortedMap<Long, String> last = treeMap.tailMap(hash);
        // 如果查询到的节点数大于0
        if(!last.isEmpty()) {
            return last.get(last.firstKey());
        }
        // 如果为0则抛出一个异常
        if (treeMap.size() == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE) ;
        }
        return treeMap.firstEntry().getValue();
    }


    @Override
    protected void processBefore() {
        treeMap.clear();
    }
}
