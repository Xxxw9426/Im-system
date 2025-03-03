package com.lld.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.FriendShipErrorCode;
import com.lld.im.common.enums.FriendShipStatusEnum;
import com.lld.im.service.friendship.dao.ImFriendShipEntity;
import com.lld.im.service.friendship.dao.mapper.ImFriendShipMapper;
import com.lld.im.service.friendship.model.req.AddFriendReq;
import com.lld.im.service.friendship.model.req.FriendDto;
import com.lld.im.service.friendship.model.req.ImportFriendShipReq;
import com.lld.im.service.friendship.model.resp.ImportFriendShipResp;
import com.lld.im.service.friendship.service.ImFriendService;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.service.ImUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 关系链模块的好友关系业务逻辑实现类
 * @Version: 1.0
 */

@Service
public class ImFriendServiceImpl implements ImFriendService {

    @Autowired
    ImFriendShipMapper imFriendShipMapper;

    @Autowired
    ImUserService imUserService;

    /***
     *  导入好友关系链
     * @param req
     * @return
     */
    @Override
    public ResponseVO importFriendShip(ImportFriendShipReq req) {

        // 进行前置校验
        if(req.getFriendItem().size()>100) {
             // 返回超出长度限制
            return ResponseVO.errorResponse(FriendShipErrorCode.IMPORT_SIZE_BEYOND);
        }

        // 要返回的导入成功的用户id集合
        List<String> successId = new ArrayList<>();
        // 要返回的导入失败的用户id集合
        List<String> errorId=new ArrayList<>();

        // 导入数据
        for(ImportFriendShipReq.ImportFriendDto dto:req.getFriendItem()){

            // 创建我们要写入数据库的实体类对象
           ImFriendShipEntity entity = new ImFriendShipEntity();
            // 将传入的to者的相关信息导入entity
            BeanUtils.copyProperties(dto,entity);
            entity.setAppId(req.getAppId());
            entity.setFromId(req.getFromId());
            try{
                int insert = imFriendShipMapper.insert(entity);
                if(insert==1) {
                    successId.add(dto.getToId());
                } else {
                    errorId.add(dto.getToId());
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                errorId.add(dto.getToId());
            }
        }

        // 封装返回结果
        ImportFriendShipResp resp = new ImportFriendShipResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);
        return ResponseVO.successResponse(resp);
    }


    /***
     * 添加好友
     * @param req
     * @return
     */
    @Override
    public ResponseVO addFriend(AddFriendReq req) {

        // 首先通过user模块中获取单个用户信息的方法判断要添加好友的用户是否存在
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()) {
            // 直接将错误信息返回
            return fromInfo;
        }

        // 然后通过user模块中获取单个用户信息的方法判断被添加好友的用户是否存在
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if(!toInfo.isOk()) {
            // 直接将错误信息返回
            return toInfo;
        }
        return doAddFriend(req.getFromId(),req.getToItem(),req.getAppId());
    }


    /**
     *  添加好友的业务方法
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    @Transactional
    public ResponseVO doAddFriend(String fromId, FriendDto dto, Integer appId) {

        // 向friend表插入A和B两天记录
        // 首先判断好友记录是否存在，如果存在，则提示已添加，如果未添加，则写入数据库
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("from_id", fromId);
        query.eq("app_id", appId);
        query.eq("to_id", dto.getToId());
        ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
        // 不存在好友记录的话将好友添加进数据库
        if(entity==null) {
            entity=new ImFriendShipEntity();
            BeanUtils.copyProperties(dto,entity);
            entity.setAppId(appId);
            entity.setFromId(fromId);
            entity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            entity.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(entity);
            if(insert!=1) {
                // 返回添加失败
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
        // 存在的话 判断状态
        } else {
            // 如果好友关系的状态是已添加
            if(entity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                // 返回已添加
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            }
            // 如果是已删除状态
            if(entity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode()) {

                // 更新好友关系中的字段值
                ImFriendShipEntity update = new ImFriendShipEntity();
                // 更新addSource
                if(StringUtils.isNotBlank(dto.getAddSource())) {
                    update.setAddSource(dto.getAddSource());
                }
                // 更新remark
                if(StringUtils.isNotBlank(dto.getRemark())) {
                    update.setRemark(dto.getRemark());
                }
                // 更新extra
                if(StringUtils.isNotBlank(dto.getExtra())) {
                    update.setExtra(dto.getExtra());
                }
                // 更新好友关系状态
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                int res = imFriendShipMapper.update(update, query);
                if(res!=1) {
                    // 返回添加失败
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
            }
        }

        QueryWrapper<ImFriendShipEntity> toQuery = new QueryWrapper<>();
        toQuery.eq("app_id",appId);
        toQuery.eq("from_id",dto.getToId());
        toQuery.eq("to_id",fromId);
        ImFriendShipEntity toItem = imFriendShipMapper.selectOne(toQuery);
        if(toItem == null){
            toItem = new ImFriendShipEntity();
            toItem.setAppId(appId);
            toItem.setFromId(dto.getToId());
            BeanUtils.copyProperties(dto,toItem);
            toItem.setToId(fromId);
            toItem.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            toItem.setCreateTime(System.currentTimeMillis());
//            toItem.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            int insert = imFriendShipMapper.insert(toItem);
        }else{
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() !=
                    toItem.getStatus()){
                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                imFriendShipMapper.update(update,toQuery);
            }
        }
        return ResponseVO.successResponse();
    }
}
