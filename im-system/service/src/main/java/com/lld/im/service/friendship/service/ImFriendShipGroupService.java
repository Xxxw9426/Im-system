package com.lld.im.service.friendship.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.lld.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.lld.im.service.friendship.model.req.DeleteFriendShipGroupReq;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-06
 * @Description: 好友分组模块的业务逻辑接口
 * @Version: 1.0
 */

public interface ImFriendShipGroupService {

    /***
     *  创建好友分组
     * @param req
     * @return
     */
    public ResponseVO addGroup(AddFriendShipGroupReq req);


    /***
     * 根据组名和拥有者id获取组的信息
     * @param fromId
     * @param groupName
     * @param appId
     * @return
     */
    public ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId,String groupName,Integer appId);


    /***
     * 删除好友分组并删除分组下的所有用户
     * @param req
     * @return
     */
    public ResponseVO deleteGroup(DeleteFriendShipGroupReq req);
}
