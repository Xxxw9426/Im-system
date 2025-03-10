package com.lld.im.service.group.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.group.model.req.GroupMemberDto;
import com.lld.im.service.group.model.req.ImportGroupMemberReq;
import com.lld.im.service.group.model.resp.GetRoleInGroupResp;

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
     * 根据传入的群组id和appId获取群组成员(内部调用)
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId);
}
