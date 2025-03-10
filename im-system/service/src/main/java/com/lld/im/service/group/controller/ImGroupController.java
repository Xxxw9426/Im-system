package com.lld.im.service.group.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.group.model.req.CreateGroupReq;
import com.lld.im.service.group.model.req.GetGroupInfoReq;
import com.lld.im.service.group.model.req.ImportGroupReq;
import com.lld.im.service.group.model.req.UpdateGroupReq;
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
     *  获取群组信息
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/getGroupInfo")
    public ResponseVO getGroupInfo(@RequestBody @Validated GetGroupInfoReq req, Integer appId)  {
        req.setAppId(appId);
        return imGroupService.getGroupInfo(req);
    }
}
