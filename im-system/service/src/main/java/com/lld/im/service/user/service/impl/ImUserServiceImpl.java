package com.lld.im.service.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.enums.DelFlagEnum;
import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.dao.mapper.ImUserDataMapper;
import com.lld.im.service.user.model.req.DeleteUserReq;
import com.lld.im.service.user.model.req.GetUserInfoReq;
import com.lld.im.service.user.model.req.ImportUserReq;
import com.lld.im.service.user.model.req.ModifyUserInfoReq;
import com.lld.im.service.user.model.resp.GetUserInfoResp;
import com.lld.im.service.user.model.resp.ImportUserResp;
import com.lld.im.service.user.service.ImUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
*@Author: 萱子王
*@CreateTime: 2025-03-01
*@Description: 用户模块业务逻辑的接口实现类
*@Version: 1.0
*/

@Service
public class ImUserServiceImpl implements ImUserService {


    @Autowired
    ImUserDataMapper imUserDataMapper;


    /***
     * 批量导入用户资料，并且返回给商户导入用户资料的详细结果
     * @param req
     * @return
     */
    @Override
    public ResponseVO importUser(ImportUserReq req) {

        // 前置校验：如果批量导入的数据过多会提示报错，限制为100
        if(req.getUserData().size() > 100){
            return ResponseVO.errorResponse(UserErrorCode.IMPORT_SIZE_BEYOND);
        }

        // 创建存储返回信息的对象
        // 导入成功的用户id
        List<String> successId = new ArrayList<>();
        // 导入失败的用户id
        List<String> errorId = new ArrayList<>();

        // forEach
        /*req.getUserData().forEach( e -> {
            try {
                // 将从前端传过来的appId添加到用户资料中
                e.setAppId(req.getAppId());
                int insert = imUserDataMapper.insert(e);
                if(insert == 1){
                    successId.add(e.getUserId());
                }
            } catch (Exception ex){
                ex.printStackTrace();
                errorId.add(e.getUserId());
            }
        });*/


        // 增强for
        for (ImUserDataEntity data: req.getUserData()) {
            try {
                // 将一个APPId的数据导入到另一个APPId中
                data.setAppId(req.getAppId());
                int insert = imUserDataMapper.insert(data);
                if(insert == 1){
                    successId.add(data.getUserId());
                }
            } catch (Exception e){
                e.printStackTrace();
                errorId.add(data.getUserId());
            }
        }

        // 设置返回信息
        ImportUserResp resp = new ImportUserResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);
        return ResponseVO.successResponse(resp);
    }


    /***
     *  批量查找用户资料
     * @param req
     * @return
     */
    @Override
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {

        // 数据库查询
        QueryWrapper<ImUserDataEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("app_id", req.getAppId());
        // 使用in批量查询
        queryWrapper.in("user_id",req.getUserIds());
        // del_flag使用枚举类
        queryWrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());
        List<ImUserDataEntity> userDataEntities=imUserDataMapper.selectList(queryWrapper);

        // 对查询到的结果进行处理
        HashMap<String,ImUserDataEntity> map=new HashMap<>();
        // 将查询到的所有用户数据存入map
        for(ImUserDataEntity data:userDataEntities){
            map.put(data.getUserId(),data);
        }
        // 判断查询失败的用户id并存储起来
        List<String> failUser=new ArrayList<>();
        for(String uid:req.getUserIds()){
            if(!map.containsKey(uid)){
                failUser.add(uid);
            }
        }

        // 封装查询到的结果
        GetUserInfoResp resp = new GetUserInfoResp();
        resp.setUserDataItem(userDataEntities);
        resp.setFailUser(failUser);
        return ResponseVO.successResponse(resp);
    }


    /***
     * 查找单个用户资料
     * @param userId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId) {
        QueryWrapper queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("app_id", appId);
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        ImUserDataEntity imUserDataEntity=imUserDataMapper.selectOne(queryWrapper);
        if(imUserDataEntity==null){
            // 用户为空返回状态码
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(imUserDataEntity);
    }


    /***
     * 批量删除用户资料
     * @param req
     * @return
     */
    @Override
    public ResponseVO deleteUser(DeleteUserReq req) {

        // 设置实体类，将其设置为已删除标志，后续mybatis会根据entity中的非空字段更新数据库
        ImUserDataEntity entity=new ImUserDataEntity();
        entity.setDelFlag(DelFlagEnum.DELETE.getCode());

        // 要返回的删除成功的用户id集合
        List<String> successId=new ArrayList<>();
        // 要返回的删除失败的用户id集合
        List<String> errorId=new ArrayList<>();

        // 查询符合条件的数据
        for(String userId:req.getUserId()) {

            QueryWrapper wrapper=new QueryWrapper<>();
            wrapper.eq("app_id", req.getAppId());
            wrapper.eq("user_id", userId);
            wrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());
            int update=0;

            // 进行逻辑删除并且收集要返回的数据
            try{
                update = imUserDataMapper.update(entity,wrapper);
                if(update>0) {
                    successId.add(userId);
                } else {
                    errorId.add(userId);
                }
            } catch (Exception e) {
                errorId.add(userId);
            }
        }

        // 封装返回的数据
        ImportUserResp resp = new ImportUserResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);
        return ResponseVO.successResponse(resp);
    }


    /***
     * 修改单个用户资料
     * @param req
     * @return
     */
    @Override
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req) {
        // 查询满足题意的数据
        QueryWrapper queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("app_id", req.getAppId());
        queryWrapper.eq("user_id", req.getUserId());
        queryWrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());
        ImUserDataEntity imUserDataEntity=imUserDataMapper.selectOne(queryWrapper);
        if(imUserDataEntity==null){
           throw new ApplicationException(UserErrorCode.USER_IS_NOT_EXIST);
        }

        // 设置要更新的实体类，注意userId，appId不能被修改，要设为空
        ImUserDataEntity update=new ImUserDataEntity();
        // 将传入的要求改的实体类复制到更新实体类中
        BeanUtils.copyProperties(req,update);
        update.setAppId(null);
        update.setUserId(null);

        // 更新
        int update1=imUserDataMapper.update(update,queryWrapper);
        if(update1==1){
            return ResponseVO.successResponse();
        }
        throw new ApplicationException(UserErrorCode.MODIFY_USER_ERROR);
    }
}
