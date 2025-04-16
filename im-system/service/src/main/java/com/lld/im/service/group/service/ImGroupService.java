package com.lld.im.service.group.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.model.SyncReq;
import com.lld.im.service.group.dao.ImGroupEntity;
import com.lld.im.service.group.model.req.*;

import java.util.Collection;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 群组模块的业务逻辑接口
 * @Version: 1.0
 */

public interface ImGroupService {

    /***
     * 导入群组(1个)
     * @param importGroupReq
     * @return
     */
    public ResponseVO importGroup(ImportGroupReq importGroupReq);


    /***
     * 根据groupId和appId获取群聊(内部调用)
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);


    /***
     *  创建群组(这里默认如果要根据群id创建一个已经创建过且删除的群聊是不行的，即使是相同的群聊再次创建也必须是不同的id)
     * @param req
     * @return
     */
    public ResponseVO createGroup(CreateGroupReq req);


    /***
     * 修改群组信息(根据操作人身份鉴权版)
     * @param req
     * @return
     */
    public ResponseVO updateGroupInfo(UpdateGroupReq req);


    /***
     * 获取群组信息(外部接口调用)
     * @param req
     * @return
     */
    public ResponseVO getGroupInfo(GetGroupInfoReq req);


    /***
     * 获取当前用户加入的所有群聊的信息
     * @param req
     * @return
     */
    public ResponseVO getJoinedGroup(GetJoinedGroupReq req);


    /***
     *  解散群组(公开群只有群主和APP管理员可以解散群组，私有群只能由APP管理员解散群组)
     * @param req
     * @return
     */
    public ResponseVO destroyGroup(DestroyGroupReq req);


    /***
     * 转让群组
     * @param req
     * @return
     */
    public ResponseVO transferGroup(TransferGroupReq req);


    /***
     *  禁言(解禁言)群(只能APP管理员，群主或者管理员才可以禁言群)
     * @param req
     * @return
     */
    public ResponseVO muteGroup(MuteGroupReq req);


    /***
     * 群组数据增量拉取
     * @param req
     * @return
     */
    public ResponseVO syncJoinedGroupList(SyncReq req);


    /***
     * 获取传入用户加入的所有群组中的最大seq值
     * @param userId
     * @param appId
     * @return
     */
    public Long getUserGroupMaxSeq(String userId, Integer appId);
}
