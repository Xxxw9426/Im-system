package com.lld.im.service.group.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.group.model.req.*;
import com.lld.im.service.group.model.resp.GetRoleInGroupResp;

import java.util.Collection;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 群组模块群成员相关业务逻辑接口
 * @Version: 1.0
 */

public interface ImGroupMemberService {


    /***
     *  批量导入群成员
     * @param req
     * @return
     */
    public ResponseVO importGroupMember(ImportGroupMemberReq req);


    /***
     * 向群成员数据库表中插入数据的业务方法
     * @param groupId
     * @param appId
     * @param dto
     * @return
     */
    public ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto);


    /***
     *  获取传入用户在传入群聊中的身份
     * @param groupId
     * @param memberId
     * @param appId
     * @return
     */
    public ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId);


    /***
     *  获取群组成员信息(单个)
     * @param req
     * @return
     */
    public ResponseVO getGroupMemberInfo(GetGroupMemberReq req);


    /***
     * 根据传入的群组id和appId获取群组成员
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId);


    /***
     * 获取当前用户加入的所有群组id
     * @param req
     * @return
     */
    public ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req);


    /***
     *  将传入的ownerId用户设置为群主身份，将原来的群主设置为普通成员
     * @param ownerId
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO transferGroupMember(String ownerId, String groupId, Integer appId);


    /***
     * 拉人入群(如果是APP管理员则执行拉人进群操作，否则只有私有群可以拉人入群)
     * @param req
     * @return
     */
    public ResponseVO addMember(AddGroupMemberReq req);


    /***
     *  踢人出群(一个)
     * @param req
     * @return
     */
    public ResponseVO removeMember(RemoveGroupMemberReq req);


    /***
     *  删除数据库中对应的群内用户(内部调用)
     * @param groupId
     * @param appId
     * @param memberId
     * @return
     */
    public ResponseVO removeGroupMember(String groupId,Integer appId,String memberId);


    /***
     *  退出群组
     * @param req
     * @return
     */
    public ResponseVO exitGroup(ExitGroupReq req);


    /***
     *  更新群成员信息
     * @param req
     * @return
     */
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req);


    /***
     * 禁言(解禁言)群成员
     * @param req
     * @return
     */
    public ResponseVO speak(SpeakMemberReq req);


    /***
     * 获取传入群聊中的所有群成员的id集合
     * @param groupId
     * @param appId
     * @return
     */
    public List<String> getGroupMemberId(String groupId, Integer appId);


    /***
     * 获取传入群聊的群管理员
     * @param groupId
     * @param appId
     * @return
     */
    public List<GroupMemberDto> getGroupManager(String groupId, Integer appId);


    /***
     * 根据传入的群成员id查找该群成员加入的所有群组
     * @param operator
     * @param appId
     * @return
     */
    public ResponseVO<Collection<String>> syncMemberJoinedGroup(String operator, Integer appId);
}
