package com.lld.im.service.group.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.model.SyncReq;
import com.lld.im.service.group.model.req.*;
import com.lld.im.service.group.service.GroupMessageService;
import com.lld.im.service.group.service.ImGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 群组模块controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("/v1/group")
public class ImGroupController {

    @Autowired
    ImGroupService imGroupService;


    @Autowired
    GroupMessageService groupMessageService;

    /***
     *  导入群组(1个)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/importGroup")
    public ResponseVO importGroup(@RequestBody @Validated ImportGroupReq req,Integer appId,String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupService.importGroup(req);
    }


    /***
     *  更新群组信息(根据操作人身份鉴权版)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/update")
    public ResponseVO update(@RequestBody @Validated UpdateGroupReq req, Integer appId, String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupService.updateGroupInfo(req);
    }


    /***
     *  创建群组(这里默认如果要根据群id创建一个已经创建过且删除的群聊是不行的，即使是相同的群聊再次创建也必须是不同的id)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/createGroup")
    public ResponseVO createGroup(@RequestBody @Validated CreateGroupReq req, Integer appId, String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupService.createGroup(req);
    }


    /***
     *  根据groupId获取群组信息
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/getGroupInfo")
    public ResponseVO getGroupInfo(@RequestBody @Validated GetGroupInfoReq req, Integer appId)  {
        req.setAppId(appId);
        return imGroupService.getGroupInfo(req);
    }


    /***
     * 获取当前用户加入的所有群聊的信息
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/getJoinedGroup")
    public ResponseVO getJoinedGroup(@RequestBody @Validated GetJoinedGroupReq req,Integer appId, String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupService.getJoinedGroup(req);
    }


    /***
     * 解散群组(公开群只有群主和APP管理员可以解散群组，私有群只能由APP管理员解散群组)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/destroyGroup")
    public ResponseVO destroyGroup(@RequestBody @Validated DestroyGroupReq req, Integer appId, String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupService.destroyGroup(req);
    }


    /***
     * 转让群组
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/transferGroup")
    public ResponseVO transferGroup(@RequestBody @Validated TransferGroupReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupService.transferGroup(req);
    }


    /***
     * 禁言(解禁言)群(只能APP管理员，群主或者管理员才可以禁言群)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/forbidSendMessage")
    public ResponseVO forbidSendMessage(@RequestBody @Validated MuteGroupReq req, Integer appId, String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupService.muteGroup(req);
    }


    /***
     *  提供的接入IM服务的服务或者APP管理员的群聊发消息的接口
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/sendMessage")
    public ResponseVO sendMessage(@RequestBody @Validated SendGroupMessageReq req,Integer appId,String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return ResponseVO.successResponse(groupMessageService.send(req));
    }


    /***
     * 群组数据增量拉取
     * @param req
     * @param appId
     * @param identifier
     * @return
     */
    @RequestMapping("/syncJoinedGroup")
    public ResponseVO syncJoinedGroup(@RequestBody @Validated SyncReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        return imGroupService.syncJoinedGroupList(req);
    }
}
