package com.lld.im.service.group.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.group.model.req.ImportGroupMemberReq;
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

}
