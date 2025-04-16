package com.lld.im.service.friendship.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lld.im.codec.pack.friendship.*;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.AllowFriendTypeEnum;
import com.lld.im.common.enums.CheckFriendShipTypeEnum;
import com.lld.im.common.enums.FriendShipErrorCode;
import com.lld.im.common.enums.FriendShipStatusEnum;
import com.lld.im.common.enums.command.FriendshipEventCommand;
import com.lld.im.common.enums.command.UserEventCommand;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.model.RequestBase;
import com.lld.im.common.model.SyncReq;
import com.lld.im.common.model.SyncResp;
import com.lld.im.service.friendship.dao.ImFriendShipEntity;
import com.lld.im.service.friendship.dao.mapper.ImFriendShipMapper;
import com.lld.im.service.friendship.model.callback.AddFriendAfterCallbackDto;
import com.lld.im.service.friendship.model.callback.AddFriendBlackAfterCallbackDto;
import com.lld.im.service.friendship.model.callback.DeleteFriendAfterCallbackDto;
import com.lld.im.service.friendship.model.req.*;
import com.lld.im.service.friendship.model.resp.CheckFriendShipResp;
import com.lld.im.service.friendship.model.resp.ImportFriendShipResp;
import com.lld.im.service.friendship.service.ImFriendShipRequestService;
import com.lld.im.service.friendship.service.ImFriendShipService;
import com.lld.im.service.seq.RedisSeq;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.service.ImUserService;
import com.lld.im.service.utils.CallbackService;
import com.lld.im.service.utils.MessageProducer;
import com.lld.im.service.utils.WriteUserSeq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 关系链模块的好友关系业务逻辑实现类
 * @Version: 1.0
 */

@Service
public class ImFriendShipServiceImpl implements ImFriendShipService {

    @Autowired
    ImFriendShipMapper imFriendShipMapper;


    @Autowired
    ImUserService imUserService;


    @Autowired
    ImFriendShipRequestService imFriendShipRequestService;


    @Autowired
    AppConfig appConfig;


    @Autowired
    CallbackService callbackService;


    @Autowired
    MessageProducer messageProducer;


    @Autowired
    RedisSeq redisSeq;


    @Autowired
    WriteUserSeq writeUserSeq;


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

        // 之前回调
        if(appConfig.isAddFriendBeforeCallback()) {
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.AddFriendBefore, JSONObject.toJSONString(req));
            // 如果回调返回的结果为失败，结束业务
            if(!responseVO.isOk()) {
                return responseVO;
            }
        }

        // 获取被添加好友用户的添加好友方式
        ImUserDataEntity data = toInfo.getData();
        // 如果被添加好友用户的添加好友方式不为空
        // 添加好友方式为1：直接添加
        if(data.getFriendAllowType()!=null && data.getFriendAllowType()== AllowFriendTypeEnum.NOT_NEED.getCode()) {

            return doAddFriend(req,req.getFromId(),req.getToItem(),req.getAppId());

        // 添加好友方式为0：申请添加
        } else {
            // 申请流程
            // 首先判断B->A好友记录是否存在，如果存在，则提示已添加，如果未添加，则写入数据库
            QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
            query.eq("from_id", req.getFromId());
            query.eq("app_id", req.getAppId());
            query.eq("to_id", req.getToItem().getToId());
            ImFriendShipEntity friendItem = imFriendShipMapper.selectOne(query);
            // 如果当前两个用户不是好友或者好友状态不是正常
            if(friendItem==null || friendItem.getStatus()!=FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                // 插入一条好友申请的数据
                ResponseVO responseVO = imFriendShipRequestService.addFriendShipRequest(req.getFromId(), req.getToItem(), req.getAppId());
                if(!responseVO.isOk()){
                    return responseVO;
                }
            } else {
                // 否则的话说明当前两个用户已经是好友
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            }

        }
        return ResponseVO.successResponse();

    }


    /**
     *  添加好友的业务方法
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    @Transactional
    public ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId) {

        // 向friend表插入A和B的好友记录
        // 首先判断A->B好友记录是否存在，如果存在，则提示已添加，如果未添加，则写入数据库
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("from_id", fromId);
        query.eq("app_id", appId);
        query.eq("to_id", dto.getToId());
        ImFriendShipEntity fromEntity = imFriendShipMapper.selectOne(query);
        Long seq=0L;
        // 不存在好友记录的话将好友添加进数据库
        if(fromEntity==null) {
            fromEntity=new ImFriendShipEntity();
            seq=redisSeq.doGetSeq(appId+":"+Constants.SeqConstants.Friendship);
            BeanUtils.copyProperties(dto,fromEntity);
            fromEntity.setAppId(appId);
            fromEntity.setFromId(fromId);
            fromEntity.setFriendSequence(seq);
            fromEntity.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            fromEntity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            fromEntity.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(fromEntity);
            if(insert!=1) {
                // 返回添加失败
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);
        // 存在的话 判断状态
        } else {
            // 如果好友关系的状态是已添加
            if(fromEntity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                // 返回已添加
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            } else {
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
                seq=redisSeq.doGetSeq(appId+":"+Constants.SeqConstants.Friendship);
                update.setFriendSequence(seq);
                // 更新好友关系状态
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                int res = imFriendShipMapper.update(update, query);
                if(res!=1) {
                    // 返回添加失败
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
                writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);
            }
        }
        // 向friend表插入B和A的好友记录
        // 首先判断B->A好友记录是否存在，如果存在，则提示已添加，如果未添加，则写入数据库
        QueryWrapper<ImFriendShipEntity> query02=new QueryWrapper<>();
        query02.eq("from_id", dto.getToId());
        query02.eq("app_id", appId);
        query02.eq("to_id", fromId);
        ImFriendShipEntity toEntity = imFriendShipMapper.selectOne(query02);
        // 不存在好友记录的话将好友添加进数据库
        if(toEntity==null) {
            toEntity=new ImFriendShipEntity();
            BeanUtils.copyProperties(dto,toEntity);
            toEntity.setFromId(dto.getToId());
            toEntity.setFriendSequence(seq);
            toEntity.setToId(fromId);
            toEntity.setAppId(appId);
            toEntity.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            toEntity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            toEntity.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(toEntity);
            if(insert!=1) {
                // 返回添加失败
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.Friendship,seq);
        // 存在的话 判断状态
        }else{
            // 如果不是好友关系，重新更新好友关系
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() !=
                    toEntity.getStatus()){
                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                imFriendShipMapper.update(update,query02);
                writeUserSeq.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.Friendship,seq);
            }
        }

        // 添加好友成功后，向当前用户的其他在线端和被添加好友用户的所有在线端发送tcp通知
        // 发送给from用户
        AddFriendPack friendPack = new AddFriendPack();
        BeanUtils.copyProperties(fromEntity,friendPack);
        friendPack.setSequence(seq);
        if(requestBase!=null) {
            // requestBase不为空则发送给from用户除了当前端以外的在线的所有端
            messageProducer.sendToUser(fromId, requestBase.getClientType(), requestBase.getImei(),
                    FriendshipEventCommand.FRIEND_ADD,friendPack, requestBase.getAppId());
        // requestBase为空，发送给from用户在线的所有端
        } else {
            messageProducer.sendToUser(fromId,FriendshipEventCommand.FRIEND_ADD, friendPack, requestBase.getAppId());
        }

        // 发送给to用户
        AddFriendPack pack = new AddFriendPack();
        BeanUtils.copyProperties(toEntity,pack);
        pack.setSequence(seq);
        // 发送给to用户的话就是发送给to用户的所有在线客户端
        messageProducer.sendToUser(dto.getToId(),FriendshipEventCommand.FRIEND_ADD, pack, requestBase.getAppId());

        // 添加好友成功后的之后回调
        if(appConfig.isAddFriendAfterCallback()) {
            // 创建要发送的信息的实体类对象
            AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
            // 添加属性值
            callbackDto.setFromId(fromId);
            callbackDto.setToItem(dto);
            callbackService.callback(appId, Constants.CallbackCommand.AddFriendAfter,
                    JSONObject.toJSONString(callbackDto));
        }


        return ResponseVO.successResponse();
    }


    /***
     *  更新好友关系链
     * @param req
     * @return
     */
    @Override
    public ResponseVO updateFriend(UpdateFriendReq req) {

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

        ResponseVO responseVO = doUpdate(req.getFromId(), req.getToItem(), req.getAppId());
        if(responseVO.isOk()) {
            // 更新好友成功后发送tcp通知
            UpdateFriendPack pack = new UpdateFriendPack();
            pack.setRemark(req.getToItem().getRemark());
            pack.setToId(req.getToItem().getToId());
            messageProducer.sendToUser(req.getFromId(),
                    req.getClientType(),req.getImei(),FriendshipEventCommand
                            .FRIEND_UPDATE,pack,req.getAppId());

            // 更新好友成功后的之后回调
            if(appConfig.isUpdateFriendAfterCallback()) {
                // 创建要发送的信息的实体类对象
                AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
                // 添加属性值
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToItem(req.getToItem());
                callbackService.callback(req.getAppId(), Constants.CallbackCommand.UpdateFriendAfter,
                        JSONObject.toJSONString(callbackDto));
            }

        }
        return responseVO;
    }


    /***
     * 更新好友关系链的业务方法
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    @Transactional
    public ResponseVO doUpdate(String fromId, FriendDto dto, Integer appId) {

        Long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);
        // 设置执行更新的wrapper:根据联合主键查询满足题意的信息并设置更新信息
        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(ImFriendShipEntity::getAddSource, dto.getAddSource())
                .set(ImFriendShipEntity::getRemark, dto.getRemark())
                .set(ImFriendShipEntity::getFriendSequence,seq)
                .set(ImFriendShipEntity::getExtra, dto.getExtra())
                .eq(ImFriendShipEntity::getFromId, fromId)
                .eq(ImFriendShipEntity::getAppId, appId)
                .eq(ImFriendShipEntity::getToId, dto.getToId());
        // 执行更新
        int update = imFriendShipMapper.update(null, updateWrapper);
        // 更新成功
        if(update==1) {
            return ResponseVO.successResponse();
        }
        writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);
        return ResponseVO.errorResponse();

    }


    /***
     * 删除特定好友关系链
     * @param req
     * @return
     */
    @Override
    public ResponseVO deleteFriend(DeleteFriendReq req) {

        // 首先查询传入的两个用户是否为好友关系
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("from_id", req.getFromId());
        query.eq("app_id", req.getAppId());
        query.eq("to_id", req.getToId());
        ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
        if(entity==null) {
            // 返回不是好友关系
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        } else {
            // 如果是正常的好友关系执行删除操作
            if(entity.getStatus() != null && entity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                ImFriendShipEntity update = new ImFriendShipEntity();
                Long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
                imFriendShipMapper.update(update, query);
                writeUserSeq.writeUserSeq(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,seq);

                // 删除好友成功后发送tcp通知
                DeleteFriendPack pack = new DeleteFriendPack();
                pack.setToId(req.getToId());
                pack.setFromId(req.getFromId());
                pack.setSequence(seq);
                messageProducer.sendToUser(req.getFromId(),
                        req.getClientType(), req.getImei(),
                        FriendshipEventCommand.FRIEND_DELETE,
                        pack, req.getAppId());

                // 删除好友成功后的之后回调
                if(appConfig.isDeleteFriendAfterCallback()) {
                    // 创建要发送的信息的实体类对象
                    DeleteFriendAfterCallbackDto callbackDto = new DeleteFriendAfterCallbackDto();
                    // 添加属性值
                    callbackDto.setFromId(req.getFromId());
                    callbackDto.setToId(req.getToId());
                    callbackService.callback(req.getAppId(), Constants.CallbackCommand.DeleteFriendAfter,
                            JSONObject.toJSONString(callbackDto));
                }

            // 如果不是正常的好友关系则返回已删除
            } else {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }
        }
        return ResponseVO.successResponse();
    }


    /***
     * 删除所有好友关系链
     * @param req
     * @return
     */
    @Override
    public ResponseVO deleteAllFriend(DeleteAllFriendReq req) {

        // 设置根据fromId查询所有与fromId是好友状态的记录的Wrapper
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("from_id", req.getFromId());
        query.eq("app_id", req.getAppId());
        query.eq("status", FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

        // 设置更新类并且通过Wrapper执行更新删除字段的操作
        ImFriendShipEntity update=new ImFriendShipEntity();
        update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
        imFriendShipMapper.update(update, query);

        // 删除所有好友成功后发送tcp通知
        DeleteAllFriendPack pack = new DeleteAllFriendPack();
        pack.setFromId(req.getFromId());
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand.FRIEND_ALL_DELETE,
                pack, req.getAppId());

        return ResponseVO.successResponse();
    }


    /***
     * 获取特定好友关系链
     * @param req
     * @return
     */
    @Override
    public ResponseVO getRelation(GetRelationReq req) {
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("from_id", req.getFromId());
        query.eq("app_id", req.getAppId());
        query.eq("status", FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
        query.eq("to_id", req.getToId());

        ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
        // 如果entity为空：两者不是好友或者已经删除好友关系
        if(entity==null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
        }
        return ResponseVO.successResponse(entity);
    }


    /***
     * 获取所有好友关系链
     * @param req
     * @return
     */
    @Override
    public ResponseVO getAllFriendShip(GetAllFriendShipReq req) {

        // 设置根据fromId查询所有与fromId是好友状态的记录的Wrapper
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("from_id", req.getFromId());
        query.eq("app_id", req.getAppId());
        query.eq("status", FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

        return ResponseVO.successResponse(imFriendShipMapper.selectList(query));
    }


    /***
     * 批量校验好友关系链
     * @param req
     * @return
     */
    @Override
    public ResponseVO checkFriendShip(CheckFriendShipReq req) {

        // 将req中的toIds拿出来放到map里，key是toId的值，value是0
        Map<String,Integer> result=req.getToIds().stream()
                .collect(Collectors.toMap(Function.identity(),s->0));

        List<CheckFriendShipResp> res=new ArrayList<>();

        // 单向校验：只需要校验对方是否在fromId的好友列表中
        if(req.getCheckType()== CheckFriendShipTypeEnum.SINGLE.getType()) {
            res=imFriendShipMapper.checkFriendShip(req);
        // 双向校验
        } else {
           res=imFriendShipMapper.checkFriendShipBoth(req);
        }

        // 将返回的res也转成map
        Map<String,Integer> collect=res.stream()
                .collect(Collectors.toMap(CheckFriendShipResp::getToId,
                        CheckFriendShipResp::getStatus));

        // 遍历要查询的toIds，并且判断结果集中是否含有要检验好友关系的toId，如果没有说明查询失败，但是也要把这个toId加入结果集
        for(String toId:result.keySet()) {
            if(! collect.containsKey(toId)) {
                CheckFriendShipResp resp=new CheckFriendShipResp();
                resp.setToId(toId);
                resp.setFromId(req.getFromId());
                // 将查询失败的toId状态设置为0，即无好友关系
                resp.setStatus(result.get(toId));
                res.add(resp);
            }
        };
        return ResponseVO.successResponse(res);
    }


    /***
     * 添加黑名单
     * @param req
     * @return
     */
    @Override
    public ResponseVO addBlack(AddFriendShipBlackReq req) {

        // 首先通过user模块中获取单个用户信息的方法判断要添加好友的用户是否存在
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()) {
            // 直接将错误信息返回
            return fromInfo;
        }

        // 然后通过user模块中获取单个用户信息的方法判断被添加好友的用户是否存在
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToId(), req.getAppId());
        if(!toInfo.isOk()) {
            // 直接将错误信息返回
            return toInfo;
        }

        // 两个用户都存在:根据三个联合主键查询是否有对应的好友记录
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("to_id", req.getToId());

        ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
        Long seq=0L;
        // 如果不存在好友记录：拉入黑名单
        if(entity==null) {
            seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
            entity = new ImFriendShipEntity();
            entity.setAppId(req.getAppId());
            entity.setFriendSequence(seq);
            entity.setFromId(req.getFromId());
            entity.setToId(req.getToId());
            //entity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            entity.setBlack(FriendShipStatusEnum.FRIEND_STATUS_NO_FRIEND.getCode());
            entity.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(entity);
            if(insert!=1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,seq);
        // 存在好友记录，判断好友记录状态
        } else {
            // 如果好友已经被拉黑，返回添加黑名单失败
            if(entity.getBlack()!=null && entity.getBlack()==FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
            // 好友未被拉黑，更新黑名单字段
            } else {
                seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setFriendSequence(seq);
                update.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
                int result = imFriendShipMapper.update(update, query);
                if(result!=1) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
                }
                writeUserSeq.writeUserSeq(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,seq);
            }
        }

        // 添加黑名单成功后发送tcp通知
        AddFriendBlackPack pack=new AddFriendBlackPack();
        pack.setSequence(seq);
        pack.setFromId(req.getFromId());
        pack.setToId(req.getToId());
        //发送tcp通知
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
                FriendshipEventCommand.FRIEND_BLACK_ADD, pack, req.getAppId());

        // 添加黑名单成功后的之后回调
        if(appConfig.isAddFriendShipBlackAfterCallback()) {
            // 创建要发送的信息的实体类对象
            AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
            // 添加属性值
            callbackDto.setFromId(req.getFromId());
            callbackDto.setToId(req.getToId());
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.AddBlackAfter,
                    JSONObject.toJSONString(callbackDto));
        }

        return ResponseVO.successResponse();
    }


    /***
     * 删除黑名单
     * @param req
     * @return
     */
    @Override
    public ResponseVO deleteBlack(DeleteBlackReq req) {
        // 根据三个联合主键查询好友记录
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("to_id", req.getToId());
        ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
        // 判断好友记录中的黑名单值
        // 如果当前好友记录没有加入黑名单
        if(entity.getBlack()!=null && entity.getBlack()==FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()) {
            throw new ApplicationException(FriendShipErrorCode.FRIEND_IS_NOT_YOUR_BLACK);
        }
        // 如果在黑名单内，修改黑名单字段
        Long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setFriendSequence(seq);
        update.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
        int update1 = imFriendShipMapper.update(update, query);
        if(update1==1) {
            writeUserSeq.writeUserSeq(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,seq);

            // 删除黑名单成功后发送tcp通知
            DeleteBlackPack pack=new DeleteBlackPack();
            pack.setSequence(seq);
            pack.setFromId(req.getFromId());
            pack.setToId(req.getToId());
            messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand.FRIEND_BLACK_DELETE,
                    pack, req.getAppId());

            // 删除黑名单成功后的之后回调
            if(appConfig.isDeleteFriendShipBlackAfterCallback()) {
                // 创建要发送的信息的实体类对象
                AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
                // 添加属性值
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToId(req.getToId());
                callbackService.callback(req.getAppId(), Constants.CallbackCommand.DeleteBlack,
                        JSONObject.toJSONString(callbackDto));
            }

            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse();
    }


    /***
     * 校验黑名单
     * @param req
     * @return
     */
    @Override
    public ResponseVO checkBlack(CheckFriendShipReq req) {

        // 将req中的toIds拿出来放到map里，key是toId的值，value是0
        Map<String,Integer> result=req.getToIds().stream()
                .collect(Collectors.toMap(Function.identity(),s->0));

        List<CheckFriendShipResp> res=new ArrayList<>();

        // 单向校验：只需要校验对方是否在fromId的黑名单中
        if(req.getCheckType()== CheckFriendShipTypeEnum.SINGLE.getType()) {
            res=imFriendShipMapper.checkFriendShipBlack(req);
            // 双向校验
        } else {
            res=imFriendShipMapper.checkFriendShipBlackBoth(req);
        }

        // 将返回的res也转成map
        Map<String,Integer> collect=res.stream()
                .collect(Collectors.toMap(CheckFriendShipResp::getToId,
                        CheckFriendShipResp::getStatus));

        // 遍历要查询的toIds，并且判断结果集中是否含有要检验黑名单的toId，如果没有说明查询失败，但是也要把这个toId加入结果集
        for(String toId:result.keySet()) {
            if(! collect.containsKey(toId)) {
                CheckFriendShipResp resp=new CheckFriendShipResp();
                resp.setToId(toId);
                resp.setFromId(req.getFromId());
                // 将查询失败的toId状态设置为0，即无黑名单状态
                resp.setStatus(result.get(toId));
                res.add(resp);
            }
        };
        return ResponseVO.successResponse(res);
    }


    /***
     * 好友列表增量拉取接口
     * @param req
     * @return
     */
    @Override
    public ResponseVO syncFriendshipList(SyncReq req) {
        // 首先设置每次最大拉取数量
        if(req.getMaxLimit()>100) {
            req.setMaxLimit(100);
        }
        SyncResp<ImFriendShipEntity> resp=new SyncResp<>();
        // 查询条件：seq > #{seq}  limit #{limit}
        QueryWrapper<ImFriendShipEntity> query=new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("from_id",req.getOperator());
        query.gt("friend_sequence",req.getLastSequence());
        query.last("limit "+req.getMaxLimit());
        query.orderByAsc("friend_sequence");       // 根据friendSequence排序

        List<ImFriendShipEntity> list = imFriendShipMapper.selectList(query);
        if(!CollectionUtils.isEmpty(list)) {
            // 获取我们查询到的最后一个元素
            ImFriendShipEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            // 获取当前用户的最大seq
            resp.setMaxSequence(maxSeqEntity.getFriendSequence());
            Long maxSeq = imFriendShipMapper.getFriendShipMaxSeq(req.getAppId(), req.getOperator());
            resp.setCompleted(maxSeqEntity.getFriendSequence() >= maxSeq);
            return ResponseVO.successResponse(resp);

        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }


    /**
     * 获取传入用户所有好友id
     * @param userId
     * @param appId
     * @return
     */
    @Override
    public List<String> getAllFriendId(String userId, Integer appId) {
        return imFriendShipMapper.getAllFriend(userId,appId);
    }


}
