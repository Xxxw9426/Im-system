package com.lld.im.service.friendship.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.lld.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.lld.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.lld.im.service.friendship.model.req.DeleteFriendShipGroupReq;
import com.lld.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.lld.im.service.friendship.service.ImFriendShipGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-06
 * @Description: 好友分组模块controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("v1/friendship/group")
public class ImFriendShipGroupController {

    @Autowired
    ImFriendShipGroupService imFriendShipGroupService;

    @Autowired
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;

    /***
     * 创建好友分组
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/add")
    public ResponseVO add(@RequestBody @Validated AddFriendShipGroupReq req,Integer appId) {
        req.setAppId(appId);
        return imFriendShipGroupService.addGroup(req);
    }


    /***
     * 向分组内添加成员
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/member/add")
    public ResponseVO memberAdd(@RequestBody @Validated AddFriendShipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.addGroupMember(req);
    }


    /***
     * 删除好友分组并删除分组下的所有用户
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/del")
    public ResponseVO del(@RequestBody @Validated DeleteFriendShipGroupReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipGroupService.deleteGroup(req);
    }


    /***
     * 删除分组中指定的用户集
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/member/del")
    public ResponseVO memberDel(@RequestBody @Validated DeleteFriendShipGroupMemberReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.delGroupMember(req);
    }
}
