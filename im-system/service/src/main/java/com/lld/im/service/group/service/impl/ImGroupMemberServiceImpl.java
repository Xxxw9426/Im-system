package com.lld.im.service.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.GroupErrorCode;
import com.lld.im.common.enums.GroupMemberRoleEnum;
import com.lld.im.service.group.dao.ImGroupMemberEntity;
import com.lld.im.service.group.dao.mapper.ImGroupMemberMapper;
import com.lld.im.service.group.model.req.GroupMemberDto;
import com.lld.im.service.group.model.req.ImportGroupMemberReq;
import com.lld.im.service.group.model.resp.AddMemberResp;
import com.lld.im.service.group.model.resp.GetRoleInGroupResp;
import com.lld.im.service.group.service.ImGroupMemberService;
import com.lld.im.service.group.service.ImGroupService;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.service.ImUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 群组模块群成员相关业务逻辑实现类
 * @Version: 1.0
 */

@Service
public class ImGroupMemberServiceImpl implements ImGroupMemberService {

    @Autowired
    ImGroupMemberMapper imGroupMemberMapper;

    @Autowired
    ImGroupService imGroupService;

    @Autowired
    ImUserService imUserService;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    /***
     *  批量导入群成员(批量导入群组成员的前提是当前群组存在)
     * @param req
     * @return
     */
    @Override
    public ResponseVO importGroupMember(ImportGroupMemberReq req) {
        // 用来存储本次导入过程中每个用户的导入结果
        List<AddMemberResp> resp=new ArrayList<>();
        // 首先判断当前群组是否存在
        ResponseVO group = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()) {
            return group;
        }
        // 遍历我们要加入群组的成员集合
        for(GroupMemberDto dto:req.getMembers()) {
            ResponseVO responseVO = imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
            // 设置当前用户加入群组的返回信息
            AddMemberResp addMemberResp=new AddMemberResp();
            addMemberResp.setMemberId(dto.getMemberId());
            // 如果当前用户插入成功
            if(responseVO.isOk()) {
                // 设置返回类中的返回结果为成功
                addMemberResp.setResult(0);
            // 如果插入失败并且返回信息为用户已经在群组中
            } else if(!responseVO.isOk() && GroupErrorCode.USER_IS_JOINED_GROUP.getCode()==responseVO.getCode()) {
                addMemberResp.setResult(2);
            // 其他
            } else {
                addMemberResp.setResult(1);
            }
            resp.add(addMemberResp);
        }
        return ResponseVO.successResponse(resp);
    }


    /***
     * 向群成员数据库表中插入数据的业务方法
     * @param groupId
     * @param appId
     * @param dto
     * @return
     */
    @Override
    public ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto) {
        // 首先判断当前用户是否存在
        ResponseVO<ImUserDataEntity> userInfo = imUserService.getSingleUserInfo(dto.getMemberId(), appId);
        if(!userInfo.isOk()) {
            return userInfo;
        }
        // 如果当前要加入的用户的角色不为空并且为群主
        if(dto.getRole()!=null && GroupMemberRoleEnum.OWNER.getCode()==dto.getRole()) {
            // 查询当前群聊的群主
            QueryWrapper<ImGroupMemberEntity> queryOwner=new QueryWrapper<>();
            queryOwner.eq("group_id",groupId);
            queryOwner.eq("app_id",appId);
            queryOwner.eq("role",GroupMemberRoleEnum.OWNER.getCode());
            Integer ownerNum = imGroupMemberMapper.selectCount(queryOwner);
            // 如果当前群聊存在群主，返回错误
            if(ownerNum>0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
            }
        }
        // 判断当前用户是否已经是群成员
        QueryWrapper<ImGroupMemberEntity> query=new QueryWrapper<>();
        query.eq("group_id",groupId);
        query.eq("app_id",appId);
        query.eq("member_id",dto.getMemberId());
        ImGroupMemberEntity memberDto = imGroupMemberMapper.selectOne(query);

        long now = System.currentTimeMillis();
        if(memberDto==null) {
            // 初次进群
            memberDto = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto,memberDto);
            memberDto.setGroupId(groupId);
            memberDto.setAppId(appId);
            memberDto.setJoinTime(now);
            int insert = imGroupMemberMapper.insert(memberDto);
            if(insert==1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        } else if(GroupMemberRoleEnum.LEAVE.getCode()==memberDto.getRole()) {
            // 重新进群
            memberDto = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, memberDto);
            memberDto.setJoinTime(now);
            int update = imGroupMemberMapper.update(memberDto, query);
            if(update==1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        }
        // 不满足初次进群和重新进群则说明当前用户已经在群中
        return ResponseVO.errorResponse(GroupErrorCode.USER_IS_JOINED_GROUP);
    }


    /***
     *  获取传入用户在传入群聊中的身份
     * @param groupId
     * @param memberId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId) {
        QueryWrapper<ImGroupMemberEntity> query=new QueryWrapper<>();
        query.eq("group_id",groupId);
        query.eq("member_id",memberId);
        query.eq("app_id",appId);
        ImGroupMemberEntity entity = imGroupMemberMapper.selectOne(query);
        // 如果没有查询到信息或者当前成员已经退群
        if(entity==null || entity.getRole()==GroupMemberRoleEnum.LEAVE.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }
        // 将查询到的结果封装到结果类中
        GetRoleInGroupResp resp=new GetRoleInGroupResp();
        resp.setGroupMemberId(entity.getGroupMemberId());
        resp.setMemberId(entity.getMemberId());
        resp.setRole(entity.getRole());
        resp.setSpeakDate(entity.getSpeakDate());
        // 返回
        return ResponseVO.successResponse(resp);
    }


    /***
     * 根据传入的群组id和appId获取群组成员(内部调用)
     * @param groupId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId) {
        List<GroupMemberDto> groupMember = imGroupMemberMapper.getGroupMember(appId, groupId);
        return ResponseVO.successResponse(groupMember);
    }
}
