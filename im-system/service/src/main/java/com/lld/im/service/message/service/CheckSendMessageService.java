package com.lld.im.service.message.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.common.config.AppConfig;
import com.lld.im.common.enums.*;
import com.lld.im.service.friendship.dao.ImFriendShipEntity;
import com.lld.im.service.friendship.model.req.GetRelationReq;
import com.lld.im.service.friendship.service.ImFriendShipService;
import com.lld.im.service.group.dao.ImGroupEntity;
import com.lld.im.service.group.model.resp.GetRoleInGroupResp;
import com.lld.im.service.group.service.ImGroupMemberService;
import com.lld.im.service.group.service.ImGroupService;
import com.lld.im.service.user.dao.ImUserDataEntity;
import com.lld.im.service.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-01
 * @Description: 处理消息的前置校验类
 * @Version: 1.0
 */

@Service
public class CheckSendMessageService {

    @Autowired
    ImUserService imUserService;


    @Autowired
    ImFriendShipService imFriendShipService;


    @Autowired
    AppConfig appConfig;


    @Autowired
    ImGroupService imGroupService;


    @Autowired
    ImGroupMemberService imGroupMemberService;


    /***
     * 校验发送消息的用户是否被禁用或者禁言
     * @param fromId
     * @param appId
     * @return
     */
    public ResponseVO checkSenderForbidAndMute(String fromId, Integer appId){
        // 首先获得当前用户的个人信息
        ResponseVO<ImUserDataEntity> userInfo = imUserService.getSingleUserInfo(fromId, appId);
        if(!userInfo.isOk()) {
            return userInfo;
        }
        // 取出用户信息
        ImUserDataEntity userData = userInfo.getData();
        if(userData.getForbiddenFlag()== UserForbiddenFlagEnum.FORBIBBEN.getCode()) {
            // 如果当前用户是被禁用的
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);

        } else if(userData.getSilentFlag()== UserSilentFlagEnum.MUTE.getCode()) {
            // 如果当前用户是被禁言的
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }
        return ResponseVO.successResponse();
    }


    /***
     * 检验发送消息和接收消息的用户之间是否有好友关系
     *     通过使用配置文件来指明是否需要校验好友关系和黑名单
     * @param fromId
     * @param toId
     * @param appId
     * @return
     */
    public ResponseVO checkFriendShip(String fromId,String toId,Integer appId){
        // 如果需要校验好友关系
        if(appConfig.isSendMessageCheckFriend()) {
            GetRelationReq fromReq = new GetRelationReq();
            // 首先校验from --> to是否有好友记录
            fromReq.setFromId(fromId);
            fromReq.setToId(toId);
            fromReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> fromRelation = imFriendShipService.getRelation(fromReq);
            if(!fromRelation.isOk()){
                return fromRelation;
            }
            // 然后校验to --> from是否有好友记录
            GetRelationReq toReq = new GetRelationReq();
            toReq.setFromId(toId);
            toReq.setToId(fromId);
            toReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> toRelation = imFriendShipService.getRelation(toReq);
            if(!toRelation.isOk()) {
                return toRelation;
            }
            // 如果from --> to的好友记录中的状态不是正常的，则说明消息的发送者将消息的接收者删除了
            // 此时消息的发送者不可以再向消息的接收者发送消息
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()
                    != fromRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }

            // 如果to --> from的好友记录中的状态不是正常的，则说明消息的接收者将消息的发送者删除了
            // 此时也无法发送消息
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()
                    != toRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }

            // 如果需要校验黑名单关系
            if(appConfig.isSendMessageCheckBlack()  ){
                // 如果from --> to的好友记录中的黑名单状态是黑名单
                // 那么此时消息的发送者不可以向消息的接收者发送消息
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != fromRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
                }

                // 如果to --> from的好友记录中的黑名单状态是黑名单
                // 那么from依然无法向to发送消息
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != toRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.TARGET_IS_BLACK_YOU);
                }

            }
        }
        return ResponseVO.successResponse();
    }


    /***
     * 用户发送群组消息的前置校验
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO checkGroupMessage(String fromId, String groupId, Integer appId) {

        // 首先判断当前发送消息的用户是否被禁言
        ResponseVO responseVO = checkSenderForbidAndMute(fromId, appId);
        if(!responseVO.isOk()) {
            return responseVO;
        }
        // 判断群逻辑
        // 首先获取群并判断群聊是否存在
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(groupId, appId);
        if(!group.isOk()) {
            return group;
        }
        // 判断发送消息的用户是否在群内
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(groupId, fromId, appId);
        if(!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        // 拿到消息发送者的身份
        GetRoleInGroupResp roleData = roleInGroupOne.getData();
        // 判断群是否被禁言，如果禁言，只有群管理和群主可以发言
        ImGroupEntity groupData = group.getData();
        // 如果当前群禁言并且发送消息的用户既不是群主也不是管理员
        if( groupData.getMute()==GroupMuteTypeEnum.MUTE.getCode()  && (roleData.getRole()!=GroupMemberRoleEnum.MANAGER.getCode())
                && (roleData.getRole()!=GroupMemberRoleEnum.OWNER.getCode()) ) {
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }
        // 判断个人是否禁言
        // 如果当前用户被禁言了并且禁言结束事件在当前时间之后
        if(roleData.getSpeakDate()!=null && roleData.getSpeakDate() > System.currentTimeMillis()) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }
        return ResponseVO.successResponse();
    }
}
