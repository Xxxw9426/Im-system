package com.lld.im.service.friendship.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.model.req.AddFriendReq;
import com.lld.im.service.friendship.model.req.ImportFriendShipReq;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 关系链模块的好友关系业务逻辑接口
 * @Version: 1.0
 */

public interface ImFriendService {


    /***
     *  导入好友关系链
     * @param req
     * @return
     */
    public ResponseVO importFriendShip(ImportFriendShipReq req);


    /***
     *  添加好友
     * @param req
     * @return
     */
    public ResponseVO addFriend(AddFriendReq req);
}
