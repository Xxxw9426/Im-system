package com.lld.im.service.friendship.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.model.req.*;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 关系链模块的好友关系业务逻辑接口
 * @Version: 1.0
 */

public interface ImFriendShipService {


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


    /**
     *  添加好友的业务方法
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    public ResponseVO doAddFriend(String fromId, FriendDto dto, Integer appId);


    /***
     *  更新好友关系链
     * @param req
     * @return
     */
    public ResponseVO updateFriend(UpdateFriendReq req);


    /***
     * 更新好友关系链的业务方法
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    public ResponseVO doUpdate(String fromId, FriendDto dto, Integer appId);


    /***
     * 删除特定好友关系链
     * @param req
     * @return
     */
    public ResponseVO deleteFriend(DeleteFriendReq req);


    /***
     * 删除所有好友关系链
     * @param req
     * @return
     */
    public ResponseVO deleteAllFriend(DeleteAllFriendReq req);


    /***
     * 获取特定好友关系链
     * @param req
     * @return
     */
    public ResponseVO getRelation(GetRelationReq req);


    /***
     * 获取所有好友关系链
     * @param req
     * @return
     */
    public ResponseVO getAllFriendShip(GetAllFriendShipReq req);


    /***
     * 批量校验好友关系链
     * @param req
     * @return
     */
    public ResponseVO checkFriendShip(CheckFriendShipReq req);


    /***
     * 添加黑名单
     * @param req
     * @return
     */
    public ResponseVO addBlack(AddFriendShipBlackReq req);


    /***
     * 删除黑名单
     * @param req
     * @return
     */
    public ResponseVO deleteBlack(DeleteBlackReq req);


    /***
     * 校验黑名单
     * @param req
     * @return
     */
    public ResponseVO checkBlack(CheckFriendShipReq req);
}
