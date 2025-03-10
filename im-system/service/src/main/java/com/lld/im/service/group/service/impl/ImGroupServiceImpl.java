package com.lld.im.service.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import java.util.List;
import java.util.UUID;

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
}
