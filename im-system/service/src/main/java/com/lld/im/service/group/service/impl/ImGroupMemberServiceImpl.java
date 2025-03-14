package com.lld.im.service.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.GroupErrorCode;
import com.lld.im.common.enums.GroupMemberRoleEnum;
import com.lld.im.common.enums.GroupStatusEnum;
import com.lld.im.common.enums.GroupTypeEnum;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.service.group.dao.ImGroupEntity;
import com.lld.im.service.group.dao.ImGroupMemberEntity;
import com.lld.im.service.group.dao.mapper.ImGroupMemberMapper;
import com.lld.im.service.group.model.req.*;
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
import java.util.Collection;
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
            memberDto.setRole(GroupMemberRoleEnum.ORDINARY.getCode());
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
     *  获取群组成员信息(单个)
     * @param req
     * @return
     */
    @Override
    public ResponseVO getGroupMemberInfo(GetGroupMemberReq req) {
        // 首先判断传入的群聊是否存在
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()) {
            return group;
        }
        // 调用获取群成员信息的方法返回获得的信息
        return imGroupMemberService.getGroupMember(req.getGroupId(),req.getAppId());
    }


    /***
     * 根据传入的群组id和appId获取群组成员
     * @param groupId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId) {
        List<GroupMemberDto> groupMember = imGroupMemberMapper.getGroupMember(appId, groupId);
        return ResponseVO.successResponse(groupMember);
    }


    /***
     * 获取当前用户加入的所有群组id
     * @param req
     * @return
     */
    @Override
    public ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req) {
        return ResponseVO.successResponse(imGroupMemberMapper.getJoinedGroupId(req.getAppId(),req.getMemberId()));
    }


    /**
     *  将传入的ownerId用户设置为群主身份，将原来的群主设置为ownerId用户的身份
     * @param ownerId
     * @param groupId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO transferGroupMember(String ownerId, String groupId, Integer appId) {
        // 更新旧群主
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.ORDINARY.getCode());
        UpdateWrapper<ImGroupMemberEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("app_id", appId);
        updateWrapper.eq("group_id", groupId);
        updateWrapper.eq("role", GroupMemberRoleEnum.OWNER.getCode());
        imGroupMemberMapper.update(imGroupMemberEntity, updateWrapper);

        //更新新群主
        ImGroupMemberEntity newOwner = new ImGroupMemberEntity();
        newOwner.setRole(GroupMemberRoleEnum.OWNER.getCode());
        UpdateWrapper<ImGroupMemberEntity> ownerWrapper = new UpdateWrapper<>();
        ownerWrapper.eq("app_id", appId);
        ownerWrapper.eq("group_id", groupId);
        ownerWrapper.eq("member_id", ownerId);
        imGroupMemberMapper.update(newOwner, ownerWrapper);

        return ResponseVO.successResponse();
    }


    /***
     * 拉人入群(如果是APP管理员则执行拉人进群操作，否则只有私有群可以拉人入群)
     * @param req
     * @return
     */
    @Override
    public ResponseVO addMember(AddGroupMemberReq req) {
        // 设置结果集
        List<AddMemberResp> resp=new ArrayList<>();
        // 默认当前操作为非APP管理员
        boolean isAdmin=false;
        // 判断当前群组是否存在
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        // 如果当前群组不存在
        if(!groupResp.isOk()) {
            return groupResp;
        }
        // 获得到拉进去的用户列表
        List<GroupMemberDto> memberDtos=req.getMembers();
        // 获取群组信息
        ImGroupEntity group=groupResp.getData();
        /**
         * 私有群（private）	类似普通微信群，创建后仅支持已在群内的好友邀请加群，且无需被邀请方同意或群主审批
         * 公开群（Public）	类似 QQ 群，创建后群主可以指定群管理员，需要群主或管理员审批通过才能入群
         * 群类型 1私有群（类似微信） 2公开群(类似qq）
         */
        // 如果当前群聊是公开群并且当前用户不是APP管理员
        if(!isAdmin && GroupTypeEnum.PUBLIC.getCode()==group.getGroupType()) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
        }
        // 成功拉进去用户id集合
        List<String> successId=new ArrayList<>();
        // 遍历要拉进去的用户
        for(GroupMemberDto memberDto:memberDtos) {
            ResponseVO responseVO = null;
            try{
                // 调用groupMemberService方法添加群组成员
                responseVO = imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(),memberDto);
            }catch(Exception e) {
                e.printStackTrace();
                responseVO=ResponseVO.errorResponse();
            }
            // 设置当前用户的返回信息对象
            AddMemberResp addMemberResp=new AddMemberResp();
            addMemberResp.setMemberId(memberDto.getMemberId());
            // 如果拉入成功
            if (responseVO.isOk()) {
                successId.add(memberDto.getMemberId());
                addMemberResp.setResult(0);
            // 用户已经在群组中
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
                addMemberResp.setResultMessage(responseVO.getMsg());
            // 拉入失败
            } else {
                addMemberResp.setResult(1);
                addMemberResp.setResultMessage(responseVO.getMsg());
            }
            // 将当前用户的返回信息加入最终结果集
            resp.add(addMemberResp);
        }
        // 返回
        return ResponseVO.successResponse(resp);
    }


    /***
     * 踢人出群(一个)
     * @param req
     * @return
     */
    @Override
    public ResponseVO removeMember(RemoveGroupMemberReq req) {
        // 首先判断当前群聊是否存在
        // 默认当前的操作用户非APP管理员
        boolean isAdmin=false;
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        // 如果当前群聊不存在
        if(!groupResp.isOk()) {
            return groupResp;
        }
        // 获得群聊的相关信息
        ImGroupEntity group=groupResp.getData();
        // 获取被踢人在群聊中的身份
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
        // 如果被踢人不在群聊中
        if(!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        // 获取被踢人的身份
        GetRoleInGroupResp memberRole=roleInGroupOne.getData();
        //获取操作人的权限 是管理员or群主or群成员
        ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
        // 如果群聊中不存在当前用户，返回错误
        if(!role.isOk()) {
            return role;
        }
        GetRoleInGroupResp data=role.getData();
        // 当前用户的身份
        Integer roleInfo=data.getRole();
        // 判断当前用户的身份
        boolean isOwner=roleInfo == GroupMemberRoleEnum.OWNER.getCode();
        boolean isManager = roleInfo == GroupMemberRoleEnum.MANAGER.getCode();
        // 进行身份校验
        if(!isAdmin) {
            // 如果当前群聊是公开群
            if(GroupTypeEnum.PUBLIC.getCode()==group.getGroupType()) {
                // 对于公开群，群主和管理员可以踢人，但是管理员只能踢普通用户
                if (!isOwner && !isManager) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }
                // 如果被踢用户是群主，返回群主不可被踢
                if(memberRole.getRole()==GroupMemberRoleEnum.OWNER.getCode()) {
                    throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
                }
                // 当前用户是管理员并且被踢人不是群成员，返回无法操作
                if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }
            // 如果当前群聊是私有群
            } else {
                // 对于私有群，只有群主可以踢人
                if(!isOwner) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }
                // 如果被踢用户是群主，返回群主不可被踢
                if(memberRole.getRole()==GroupMemberRoleEnum.OWNER.getCode()) {
                    throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
                }
            }
        }
        ResponseVO responseVO = imGroupMemberService.removeGroupMember(req.getGroupId(), req.getAppId(), req.getMemberId());
        return responseVO;
    }


    /***
     * 删除数据库中对应的群内用户(内部调用)
     * @param groupId
     * @param appId
     * @param memberId
     * @return
     */
    @Override
    public ResponseVO removeGroupMember(String groupId,Integer appId,String memberId) {
        // 首先判断当前用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(memberId, appId);
        // 如果当前用户不存在
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }
        // 判断当前用户在群聊中的身份
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(groupId, memberId, appId);
        // 如果当前用户不在群聊中
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        // 更新用户在群聊中的状态为离开
        GetRoleInGroupResp data = roleInGroupOne.getData();
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        imGroupMemberEntity.setLeaveTime(System.currentTimeMillis());
        imGroupMemberEntity.setGroupMemberId(data.getGroupMemberId());
        imGroupMemberMapper.updateById(imGroupMemberEntity);
        return ResponseVO.successResponse();
    }


    /***
     * 退出群聊
     * @param req
     * @return
     */
    @Override
    public ResponseVO exitGroup(ExitGroupReq req) {
        // 判断当前群聊是否存在
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        // 如果不存在，返回错误
        if(!group.isOk()) {
            return group;
        }
        // 判断当前用户在群聊中的身份
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
        // 如果当前用户不在群聊中
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        // 判断用户在群聊中的身份
        // 私有群支持群主退群，退群后群处于无群主状态
        // 公开群不支持群主退群
        if(group.getData().getGroupType()==GroupTypeEnum.PUBLIC.getCode() && roleInGroupOne.getData().getRole()==GroupMemberRoleEnum.OWNER.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
        }
        // 更新请求退群用户的状态为离群
        ImGroupMemberEntity update=new ImGroupMemberEntity();
        update.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        update.setLeaveTime(System.currentTimeMillis());
        UpdateWrapper<ImGroupMemberEntity> updateWrapper=new UpdateWrapper<>();
        updateWrapper.eq("app_id",req.getAppId());
        updateWrapper.eq("group_id",group.getData().getGroupId());
        updateWrapper.eq("member_id",req.getOperator());
        imGroupMemberMapper.update(update,updateWrapper);
        return ResponseVO.successResponse();
    }


    /***
     * 更新群成员信息
     * @param req
     * @return
     */
    @Override
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req) {
        // 默认为非APP管理员操作
        boolean isAdmin=false;
        // 判断当前群聊记录是否存在并且获得当前群聊的信息
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()) {
            return group;
        }
        ImGroupEntity groupData = group.getData();
        // 如果当前群聊已经解散
        if(groupData.getStatus()== GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        // 判断是否为当前用户修改自己的群组内资料
        boolean isMeOperate=req.getOperator().equals(req.getMemberId());

        // 进行身份校验
        if(!isAdmin) {
            // TODO 首先，用户的群昵称只能由用户自身修改，用户的权限可以有群主和管理员修改
            // 如果群昵称字段不为空并且不是用户自身操作
            if( !isMeOperate && !StringUtils.isBlank(req.getAlias())){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_ONESELF);
            }
            // 对修改权限操作进行前置校验
            if(req.getRole()!=null) {
                // 私有群不能设置管理员，也不能设群主
                if (groupData.getGroupType() == GroupTypeEnum.PRIVATE.getCode() &&
                        req.getRole() != null && (req.getRole() == GroupMemberRoleEnum.MANAGER.getCode() ||
                        req.getRole() == GroupMemberRoleEnum.OWNER.getCode())) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
                }

                // 获取被操作人的是否在群内
                ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                // 不在群内返回错误
                if (!roleInGroupOne.isOk()) {
                    return roleInGroupOne;
                }

                // 获取操作人权限
                ResponseVO<GetRoleInGroupResp> operateRoleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
                // 操作人信息未查询到返回错误
                if (!operateRoleInGroupOne.isOk()) {
                    return operateRoleInGroupOne;
                }

                GetRoleInGroupResp data = operateRoleInGroupOne.getData();
                // 获取操作人的身份
                Integer roleInfo = data.getRole();
                boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
                boolean isManager = roleInfo == GroupMemberRoleEnum.MANAGER.getCode();

                // 不是管理员或者群主不能修改权限
                if (req.getRole() != null && !isOwner && !isManager) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                // 管理员只有群主能够设置
                if (req.getRole() != null && req.getRole() == GroupMemberRoleEnum.MANAGER.getCode() && !isOwner) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }

                // 不能将其他人直接修改成群主，群主只能通过转让的方式
                if(req.getRole()!=null && req.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                    return ResponseVO.errorResponse(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
                }
            }
        }
        // 开始执行更新
        ImGroupMemberEntity update = new ImGroupMemberEntity();
        // 如果群昵称不为空则设置群昵称
        if (StringUtils.isNotBlank(req.getAlias())) {
            update.setAlias(req.getAlias());
        }
        // 修改要被修改人的身份信息：但是不能直接被修改为群主，群主只能转让
        if(req.getRole() != null && req.getRole() != GroupMemberRoleEnum.OWNER.getCode()){
            update.setRole(req.getRole());
        }

        // 执行更新
        UpdateWrapper<ImGroupMemberEntity> objectUpdateWrapper = new UpdateWrapper<>();
        objectUpdateWrapper.eq("app_id", req.getAppId());
        objectUpdateWrapper.eq("member_id", req.getMemberId());
        objectUpdateWrapper.eq("group_id", req.getGroupId());
        imGroupMemberMapper.update(update, objectUpdateWrapper);
        // 返回结果
        return ResponseVO.successResponse();
    }


    /***
     * 禁言(解禁言)群成员
     * @param req
     * @return
     */
    @Override
    public ResponseVO speak(SpeakMemberReq req) {
        // 首先判断传入群聊是否存在
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        // 如果不存在则返回错误
        if (!groupResp.isOk()) {
            return groupResp;
        }
        // 默认非APP管理员操作
        boolean isAdmin = false;
        // 进行权限校验
        boolean isOwner = false;
        boolean isManager = false;
        GetRoleInGroupResp memberRole = null;
        if(!isAdmin) {
            // 获取当前操作人的权限 是管理员or群主or群成员
            ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            // 当前操作人已经不在群内
            if (!role.isOk()) {
                return role;
            }
            // 获取当前操作人的身份
            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
            isManager = roleInfo == GroupMemberRoleEnum.MANAGER.getCode();

            // 如果既不是群主也不是管理员则不能禁言群成员
            if (!isOwner && !isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            // 获取被操作人的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            // 被操作人已经不在群内
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            // 获取被操作人的身份
            memberRole = roleInGroupOne.getData();

            // 如果被操作人是群主只能app管理员操作
            if (memberRole.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
            }

            // 如果当前操作人是管理员并且被操作人不是普通群成员，无法操作
            if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        if (memberRole == null) {
            //获取被操作人的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
        }
        imGroupMemberEntity.setGroupMemberId(memberRole.getGroupMemberId());
        // 如果设置了禁言期限则在当前时间基础上加上期限，否则直接设置即为解禁言
        if (req.getSpeakDate() > 0) {
            imGroupMemberEntity.setSpeakDate(System.currentTimeMillis() + req.getSpeakDate());
        } else {
            imGroupMemberEntity.setSpeakDate(req.getSpeakDate());
        }
        // 更新
        int i = imGroupMemberMapper.updateById(imGroupMemberEntity);
        return ResponseVO.successResponse();
    }
}
