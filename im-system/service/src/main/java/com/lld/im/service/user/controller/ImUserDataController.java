package com.lld.im.service.user.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.user.model.req.DeleteUserReq;
import com.lld.im.service.user.model.req.GetUserInfoReq;
import com.lld.im.service.user.model.req.ModifyUserInfoReq;
import com.lld.im.service.user.model.req.UserId;
import com.lld.im.service.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-01
 * @Description: 用户模块处理用户数据的controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("v1/user/data")
public class ImUserDataController {

    @Autowired
    ImUserService imUserService;

    /***
     * 批量查找用户资料
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(@RequestBody GetUserInfoReq req,Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserInfo(req);
    }


    /***
     * 批量删除用户资料
     * @param req
     * @param appId
     * @return
     */
    // TODO 使用@Validated注解来触发DeleteUserReq对象中字段对应注解的验证
    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.deleteUser(req);
    }


    /***
     * 获取单个用户资料
     * @param req
     * @param appId
     * @return
     */
    // TODO 使用@Validated注解来触发UserId对象中字段对应注解的验证
    @RequestMapping("/getSingleUserInfo")
    public ResponseVO getSingleUserInfo(@RequestBody @Validated UserId req , Integer appId) {
        req.setAppId(appId);
        return imUserService.getSingleUserInfo(req.getUserId(),appId);
    }


    /***
     *  修改单个用户资料
     * @param req
     * @param appId
     * @return
     */
    // TODO 使用@Validated注解来触发ModifyUserInfoReq对象中字段对应注解的验证
    @RequestMapping("/modifyUserInfo")
    public ResponseVO modifyUserInfo(@RequestBody @Validated ModifyUserInfoReq req,Integer appId) {
        req.setAppId(appId);
        return imUserService.modifyUserInfo(req);
    }


}
