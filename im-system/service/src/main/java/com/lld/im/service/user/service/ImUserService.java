package com.lld.im.service.user.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.model.req.DeleteUserReq;
import com.lld.im.service.user.model.req.GetUserInfoReq;
import com.lld.im.service.user.model.req.ImportUserReq;
import com.lld.im.service.user.model.req.ModifyUserInfoReq;
import com.lld.im.service.user.model.resp.GetUserInfoResp;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-01
 * @Description: 用户模块业务逻辑的接口
 * @Version: 1.0
 */

public interface ImUserService {

    /***
     * 批量导入用户资料，并且返回给商户导入用户资料的详细结果
     * @param req
     * @return
     */
    public ResponseVO importUser(ImportUserReq req);


    /***
     *  批量查找用户资料
     * @param req
     * @return
     */
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);


    /***
     * 查找单个用户资料
     * @param userId
     * @param appId
     * @return
     */
    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId,Integer appId);


    /***
     * 批量删除用户资料
     * @param req
     * @return
     */
    public ResponseVO deleteUser(DeleteUserReq req);


    /***
     * 修改单个用户资料
     * @param req
     * @return
     */
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req);
}
