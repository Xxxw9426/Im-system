package com.lld.im.service.group.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.group.model.req.*;
import com.lld.im.service.group.service.ImGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 群组模块群成员相关功能controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("v1/group/member")
public class ImGroupMemberController {

    @Autowired
    ImGroupMemberService imGroupMemberService;


    /***
     * 批量导入群成员(批量导入群组成员的前提是当前群组存在)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/importGroupMember")
    public ResponseVO importGroupMember(@RequestBody @Validated ImportGroupMemberReq req,Integer appId,String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupMemberService.importGroupMember(req);
    }


    /***
     *  拉人入群(如果是APP管理员则执行拉人进群操作，否则只有私有群可以拉人入群)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/add")
    public ResponseVO addMember(@RequestBody @Validated AddGroupMemberReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupMemberService.addMember(req);
    }


    /***
     * 踢人出群(一个)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/remove")
    public ResponseVO removeMember(@RequestBody @Validated RemoveGroupMemberReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupMemberService.removeMember(req);
    }



    /***
     *  退出群聊
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/exit")
    public ResponseVO exitGroup(@RequestBody @Validated ExitGroupReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupMemberService.exitGroup(req);
    }


    /***
     *  更新群成员信息
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/update")
    public ResponseVO updateGroupMember(@RequestBody @Validated UpdateGroupMemberReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupMemberService.updateGroupMember(req);
    }


    /***
     * 获取群组成员信息(单个)
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/getGroupMember")
    public ResponseVO getGroupMemberInfo(@RequestBody @Validated GetGroupMemberReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupMemberService.getGroupMemberInfo(req);
    }


    /***
     * 禁言(解禁言)群成员
     * @param req
     * @param appId
     * @param operator
     * @return
     */
    @RequestMapping("/speak")
    public ResponseVO speak(@RequestBody @Validated SpeakMemberReq req, Integer appId, String operator)  {
        req.setAppId(appId);
        req.setOperator(operator);
        return imGroupMemberService.speak(req);
    }

}
