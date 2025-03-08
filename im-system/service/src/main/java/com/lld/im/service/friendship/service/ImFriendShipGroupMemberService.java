package com.lld.im.service.friendship.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.lld.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-06
 * @Description: 分组成员模块的业务逻辑接口
 * @Version: 1.0
 */

public interface ImFriendShipGroupMemberService {

    /***
     * 向分组内添加成员
     * @param req
     * @return
     */
    public ResponseVO addGroupMember(AddFriendShipGroupMemberReq req);


    /***
     * 向数据库中组成员的表内插入用户
     * @param groupId
     * @param id
     * @return
     */
    public int doAddGroupMember(Long groupId, String id);


    /***
     * 根据分组id清空当前分组的所有用户
     * @param groupId
     */
    public int clearGroupMember(Long groupId);


    /***
     * 删除分组中指定的用户集
     * @param req
     * @return
     */
    ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req);
}
