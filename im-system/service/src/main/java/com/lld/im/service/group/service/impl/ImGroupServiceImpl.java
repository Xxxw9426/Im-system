package com.lld.im.service.group.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.GroupErrorCode;
import com.lld.im.common.enums.GroupMemberRoleEnum;
import com.lld.im.common.enums.GroupStatusEnum;
import com.lld.im.common.enums.GroupTypeEnum;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.service.group.dao.ImGroupEntity;
import com.lld.im.service.group.dao.mapper.ImGroupMapper;
import com.lld.im.service.group.model.req.*;
import com.lld.im.service.group.model.resp.GetGroupResp;
import com.lld.im.service.group.model.resp.GetRoleInGroupResp;
import com.lld.im.service.group.service.ImGroupMemberService;
import com.lld.im.service.group.service.ImGroupService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 群组模块的业务逻辑实现类
 * @Version: 1.0
 */

@Service
public class ImGroupServiceImpl implements ImGroupService {

    @Autowired
    ImGroupMapper imGroupMapper;


    @Autowired
    ImGroupMemberService imGroupMemberService;

    /***
     * 导入群组(1个)
     * @param req
     * @return
     */
    @Override
    public ResponseVO importGroup(ImportGroupReq req) {
        // 首先判断请求参数中groupId是否存在
        if(StringUtils.isNotBlank(req.getGroupId())) {
            // 判断数据库中是否存在当前群
            QueryWrapper<ImGroupEntity> query=new QueryWrapper<>();
            query.eq("group_id", req.getGroupId());
            query.eq("app_id", req.getAppId());

            if(imGroupMapper.selectCount(query)>0) {
                // 返回记录已存在
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_EXIST);
            }
        } else {
            // 不存在的话则由我们给当前群生成一个groupId
            req.setGroupId(UUID.randomUUID().toString().replace("-",""));
        }
        // 向数据库中插入群聊
        ImGroupEntity entity=new ImGroupEntity();
        BeanUtils.copyProperties(req,entity);
        // 补充一下参数
        if(req.getCreateTime()==null) {
            entity.setCreateTime(System.currentTimeMillis());
        }
        if(req.getStatus()==null) {
            entity.setStatus(GroupStatusEnum.NORMAL.getCode());
        }
        int insert = imGroupMapper.insert(entity);
        if(insert!=1) {
            throw new ApplicationException(GroupErrorCode.IMPORT_GROUP_ERROR);
        }
        return ResponseVO.successResponse();
    }


    /***
     * 根据groupId和appId获取群聊(内部调用)
     * @param groupId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId) {
        QueryWrapper<ImGroupEntity> query=new QueryWrapper<>();
        query.eq("group_id", groupId);
        query.eq("app_id", appId);
        ImGroupEntity entity = imGroupMapper.selectOne(query);
        if(entity==null) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }


    /***
     * 创建群组(这里默认如果要根据群id创建一个已经创建过且删除的群聊是不行的，即使是相同的群聊再次创建也必须是不同的id)
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO createGroup(CreateGroupReq req) {

        // 首先默认非管理员操作
        boolean isAdmin = false;

        // 非管理员操作将当前操作的用户设定为要创建的群聊的群主
        if (!isAdmin) {
            req.setOwnerId(req.getOperator());
        }

        // 1.判断操作者是否传入了群id
        QueryWrapper<ImGroupEntity> query = new QueryWrapper<>();

        // 没有传入的话人为生成一个群id
        if (StringUtils.isEmpty(req.getGroupId())) {
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));

        // 传入了的话根据群id查询当前数据库内是否已经存在该群聊
        } else {
            query.eq("group_id", req.getGroupId());
            query.eq("app_id", req.getAppId());
            Integer integer = imGroupMapper.selectCount(query);
            // 存在的话返回异常
            if (integer > 0) {
                throw new ApplicationException(GroupErrorCode.GROUP_IS_EXIST);
            }
        }

        // 如果当前群聊是公开群并且群主id为空
        // 抛出错误：公开群必须有群主
        if (req.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }

        // 向数据库中插入要添加的群聊的信息
        ImGroupEntity imGroupEntity = new ImGroupEntity();
        imGroupEntity.setCreateTime(System.currentTimeMillis());
        imGroupEntity.setStatus(GroupStatusEnum.NORMAL.getCode());
        BeanUtils.copyProperties(req, imGroupEntity);
        int insert = imGroupMapper.insert(imGroupEntity);

        // 将群主加入当前群聊的群成员表
        GroupMemberDto groupMemberDto = new GroupMemberDto();
        groupMemberDto.setMemberId(req.getOwnerId());
        groupMemberDto.setRole(GroupMemberRoleEnum.OWNER.getCode());
        groupMemberDto.setJoinTime(System.currentTimeMillis());
        imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), groupMemberDto);

        // 插入群成员
        for (GroupMemberDto dto : req.getMember()) {
            imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
        }

        return ResponseVO.successResponse();
    }


    /***
     * 修改群组信息(根据操作人身份鉴权版)
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO updateGroupInfo(UpdateGroupReq req) {
        // 首先判断传入的要修改群信息的群是否存在
        ResponseVO<ImGroupEntity> group = getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()) {
            return group;
        }
        // 先默认不是后台调用
        boolean isAdmin=false;
        // 不是后台调用则需要鉴权
        if(!isAdmin) {
            // 校验权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            // 如果没有当前成员或者当前成员已经不在群内
            if(!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            // 获取当前成员的身份
            GetRoleInGroupResp roleData=roleInGroupOne.getData();
            int role=roleData.getRole();
            boolean isManager=role== GroupMemberRoleEnum.MANAGER.getCode();
            boolean isOwner=role==GroupMemberRoleEnum.OWNER.getCode();
            // 判断当前群聊的类型:如果当前群聊是公开群的话只允许群主修改群聊信息，如果是私有群则任何人都可以修改资料
            if((!isManager||!isOwner) && GroupTypeEnum.PUBLIC.getCode() == group.getData().getGroupType()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
         }
        // 更新的内容
        ImGroupEntity entity = new ImGroupEntity();
        BeanUtils.copyProperties(req,entity);
        // 更新的条件
        QueryWrapper<ImGroupEntity> query=new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("group_id", req.getGroupId());
        int update = imGroupMapper.update(entity, query);
        if(update!=1) {
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }
        return ResponseVO.successResponse();
    }


    /***
     * 获取群组信息(外部接口调用)
     * @param req
     * @return
     */
    @Override
    public ResponseVO getGroupInfo(GetGroupInfoReq req) {

        // 首先判断用户传入的要查询的群组是否存在
        ResponseVO group = getGroup(req.getGroupId(), req.getAppId());
        // 不存在的话返回错误
        if(!group.isOk()){
            return group;
        }
        // 封装响应实体类
        GetGroupResp getGroupResp = new GetGroupResp();
        // 将查询到的群组信息赋值给响应实体类
        BeanUtils.copyProperties(group.getData(), getGroupResp);
        // 调用memberService中的获取群组成员的方法获取当前群组的成员
        try {
            ResponseVO<List<GroupMemberDto>> groupMember = imGroupMemberService.getGroupMember(req.getGroupId(), req.getAppId());
            if (groupMember.isOk()) {
                getGroupResp.setMemberList(groupMember.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseVO.successResponse(getGroupResp);
    }


    /***
     * 获取当前用户加入的所有群聊的信息
     * @param req
     * @return
     */
    @Override
    public ResponseVO getJoinedGroup(GetJoinedGroupReq req) {
        // 获取当前用户加入的所有群组id
        ResponseVO<Collection<String>> groupIds = imGroupMemberService.getMemberJoinedGroup(req);
        // 结果集
        List<ImGroupEntity> list=new ArrayList<>();
        QueryWrapper<ImGroupEntity> query=new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        if(!CollectionUtil.isEmpty(req.getGroupType())) {
            query.in("group_type", req.getGroupType());
        }
        query.in("group_id", groupIds.getData());
        list=imGroupMapper.selectList(query);
        // 根据群组id获取群组信息
        return ResponseVO.successResponse(list);
    }


    /***
     *  解散群组(公开群只有群主和APP管理员可以解散群组，私有群只能由APP管理员解散群组)
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO destroyGroup(DestroyGroupReq req) {
        // 默认非APP管理员操作
        boolean isAdmin=false;
        // 判断当前群组是否存在
        QueryWrapper<ImGroupEntity> groupQuery=new QueryWrapper<>();
        groupQuery.eq("app_id", req.getAppId());
        groupQuery.eq("group_id", req.getGroupId());
        ImGroupEntity groupEntity = imGroupMapper.selectOne(groupQuery);
        // 如果当前群组不存在
        if(groupEntity==null) {
            // 抛出异常
            throw new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        // 如果当前群组已经解散
        if(groupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            // 返回失败
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        // 如果不是APP管理员
        if(!isAdmin) {
            // 如果是私有群
            if(groupEntity.getStatus() == GroupTypeEnum.PRIVATE.getCode()) {
                // 返回私有群不允许解散
                throw new ApplicationException(GroupErrorCode.PRIVATE_GROUP_CAN_NOT_DESTORY);
            }
            // 如果是公开群但是非群主或管理员
            if( groupEntity.getStatus() == GroupTypeEnum.PUBLIC.getCode() && !(groupEntity.getOwnerId().equals(req.getOperator()))) {
                // 返回此操作需要群主身份
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
            // 前置校验结束，更新数据库中群组的删除字段状态
            ImGroupEntity update=new ImGroupEntity();
            update.setStatus(GroupStatusEnum.DESTROY.getCode());
            int update1 = imGroupMapper.update(update, groupQuery);
            if(update1!=1) {
                throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
            }
        }
        return ResponseVO.successResponse();
    }


    /***
     * 转让群组
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO transferGroup(TransferGroupReq req) {
        // 首先判断当前用户在传入的群组中的身份
        ResponseVO<GetRoleInGroupResp> currentUser = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
        // 如果没有查询到信息
        if(!currentUser.isOk()) {
            return currentUser;
        }
        // 如果当前用户不是群主
        if(currentUser.getData().getRole()!=GroupMemberRoleEnum.OWNER.getCode()) {
            // 返回需要群主操作
            return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
        }
        // 再判断要转让给的用户是否在群聊中
        ResponseVO<GetRoleInGroupResp> newOwner = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOwnerId(), req.getAppId());
        if(!newOwner.isOk()) {
            return newOwner;
        }
        // 判断当前群聊状态
        QueryWrapper<ImGroupEntity> currentGroup=new QueryWrapper<>();
        currentGroup.eq("group_id", req.getGroupId());
        currentGroup.eq("app_id", req.getAppId());
        ImGroupEntity groupEntity = imGroupMapper.selectOne(currentGroup);
        // 如果当前群聊已经解散
        if(groupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        // 执行转让操作
        // 这里是更新group表中的群主信息
        ImGroupEntity updateGroup = new ImGroupEntity();
        updateGroup.setOwnerId(req.getOwnerId());
        UpdateWrapper<ImGroupEntity> updateGroupWrapper = new UpdateWrapper<>();
        updateGroupWrapper.eq("app_id", req.getAppId());
        updateGroupWrapper.eq("group_id", req.getGroupId());
        imGroupMapper.update(updateGroup, updateGroupWrapper);
        // 更新group-member表中的信息
        imGroupMemberService.transferGroupMember(req.getOwnerId(),req.getGroupId(),req.getAppId());
        return ResponseVO.successResponse();
    }


    /***
     * 禁言(解禁言)群(只能APP管理员，群主或者管理员才可以禁言群)
     * @param req
     * @return
     */
    @Override
    public ResponseVO muteGroup(MuteGroupReq req) {
        // 首先判断当前群聊是否存在
        ResponseVO<ImGroupEntity> group = getGroup(req.getGroupId(), req.getAppId());
        // 当前群不存在
        if(!group.isOk()) {
            return group;
        }
        // 当前群已解散
        if(group.getData().getStatus()==GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        // 默认非APP管理员操作
        boolean isAdmin=false;
        if(!isAdmin) {
            // 校验当前用户的权限
            ResponseVO<GetRoleInGroupResp> role = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            // 如果当前用户已经不在群聊中返回错误
            if(!role.isOk()) {
                return role;
            }
            GetRoleInGroupResp data = role.getData();
            Integer roleId = data.getRole();
            // 记录当前用户是否是群主或者管理员
            boolean isManager = roleId == GroupMemberRoleEnum.MANAGER.getCode() || roleId == GroupMemberRoleEnum.OWNER.getCode();
            if(!isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }
        // 权限校验结束后执行更新
        ImGroupEntity update = new ImGroupEntity();
        update.setMute(req.getMute());

        UpdateWrapper<ImGroupEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("group_id",req.getGroupId());
        wrapper.eq("app_id",req.getAppId());
        imGroupMapper.update(update,wrapper);

        return ResponseVO.successResponse();
    }
}
