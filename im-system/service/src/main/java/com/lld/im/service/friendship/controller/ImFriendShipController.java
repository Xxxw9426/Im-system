package com.lld.im.service.friendship.controller;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.friendship.model.req.*;
import com.lld.im.service.friendship.service.ImFriendShipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 关系链模块好友关系controller层
 * @Version: 1.0
 */

@RestController
@RequestMapping("v1/friendship")
public class ImFriendShipController {

    @Autowired
    ImFriendShipService imFriendShipService;


    /***
     *  批量导入好友关系链
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/importFriendShip")
    public ResponseVO importFriendShip(@RequestBody @Validated ImportFriendShipReq req,Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.importFriendShip(req);
    }


    /***
     * 添加好友关系链
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/addFriend")
    public ResponseVO addFriend(@RequestBody @Validated AddFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.addFriend(req);
    }


    /***
     * 更新好友关系链
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/updateFriend")
    public ResponseVO updateFriend(@RequestBody @Validated UpdateFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.updateFriend(req);
    }


    /***
     * 删除特定好友关系链
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/deleteFriend")
    public ResponseVO deleteFriend(@RequestBody @Validated DeleteFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteFriend(req);
    }


    /***
     * 删除所有好友关系链
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/deleteAllFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated DeleteAllFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteAllFriend(req);
    }


    /***
     * 获取特定的好友关系链
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/getRelation")
    public ResponseVO getRelation(@RequestBody @Validated GetRelationReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.getRelation(req);
    }


    /***
     *  获取所有好友关系链
     * @param appId
     * @return
     */
    @RequestMapping("/getAllFriendShip")
    public ResponseVO getAllFriendShip(@RequestBody @Validated GetAllFriendShipReq req,Integer appId){
        req.setAppId(appId);
        return imFriendShipService.getAllFriendShip(req);
    }


    /***
     * 校验好友关系链
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/checkFriend")
    public ResponseVO checkFriend(@RequestBody @Validated CheckFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.checkFriendShip(req);
    }


    /***
     * 添加黑名单
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/addBlack")
    public ResponseVO addBlack(@RequestBody @Validated AddFriendShipBlackReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.addBlack(req);
    }


    /***
     * 删除黑名单
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/deleteBlack")
    public ResponseVO deleteBlack(@RequestBody @Validated DeleteBlackReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteBlack(req);
    }


    /***
     * 校验黑名单
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/checkBlack")
    public ResponseVO checkBlack(@RequestBody @Validated CheckFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.checkBlack(req);
    }

}
