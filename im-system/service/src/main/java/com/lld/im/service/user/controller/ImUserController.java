package com.lld.im.service.user.controller;

import com.lld.im.common.ClientType;
import com.lld.im.common.ResponseVO;
import com.lld.im.common.route.RouteHandle;
import com.lld.im.common.route.RouteInfo;
import com.lld.im.common.route.algorithm.random.RandomHandle;
import com.lld.im.common.utils.RouteInfoParseUtil;
import com.lld.im.service.user.model.req.DeleteUserReq;
import com.lld.im.service.user.model.req.ImportUserReq;
import com.lld.im.service.user.model.req.LoginReq;
import com.lld.im.service.user.service.ImUserService;
import com.lld.im.service.utils.ZKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-01
 * @Description: 用户模块的controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("v1/user")
public class ImUserController {

    @Autowired
    ImUserService imUserService;

    @Autowired
    RouteHandle routeHandle;

    @Autowired
    ZKit zKit;


    /***
     *  批量导入用户资料
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.importUser(req);
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


    /**
     * @param req
     * @return com.lld.im.common.ResponseVO
     * @description IM系统的登录接口，返回IM的地址，web端地址/tcp地址
     * @author chackylee
     */
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId) {
        req.setAppId(appId);
        ResponseVO login = imUserService.login(req);
        // TODO 从zookeeper获取一个IM的地址，返回给sdk
        if(login.isOk()) {
            List<String> allNode;
            if(req.getClientType()== ClientType.WEB.getCode()) {
               allNode = zKit.getAllWebNode();
            } else {
               allNode = zKit.getAllTcpNode();
            }
            // 这里返回的server为ip:port格式
            String server = routeHandle.routeServer(allNode, req.getUserId());
            // 将其解析为ip+port的格式并存储在实体类对象中返回
            RouteInfo ipPort = RouteInfoParseUtil.parse(server);
            return ResponseVO.successResponse(ipPort);
        }
        return ResponseVO.errorResponse();
    }
}
