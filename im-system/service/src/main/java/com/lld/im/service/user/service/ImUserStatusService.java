package com.lld.im.service.user.service;

import com.lld.im.service.user.model.UserStatusChangeNotifyContent;
import com.lld.im.service.user.model.req.PullFriendOnlineStatusReq;
import com.lld.im.service.user.model.req.PullUserOnlineStatusReq;
import com.lld.im.service.user.model.req.SetUserCustomerStatusReq;
import com.lld.im.service.user.model.req.SubscribeUserOnlineStatusReq;
import com.lld.im.service.user.model.resp.UserOnlineStatusResp;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-04-10
 * @Description: 用户模块处理tcp发送过来的消息的方法的接口
 * @Version: 1.0
 */
public interface ImUserStatusService {


    /***
     * 处理用户在线状态发生变化的方法
     * @param content
     */
    public void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content);


    /***
     * 订阅用户在线状态
     * @param req
     */
    public void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req);


    /***
     * 设置用户自身的在线状态
     * @param req
     */
    public void setUserCustomerStatus(SetUserCustomerStatusReq req);


    /***
     *  拉取当前用户的所有好友的在线状态
     * @param req
     * @return
     */
    public Map<String, UserOnlineStatusResp> queryFriendOnlineStatus(PullFriendOnlineStatusReq req);


    /***
     *  拉取传入列表中的用户的在线状态
     * @param req
     * @return
     */
    public Map<String, UserOnlineStatusResp> queryUserOnlineStatus(PullUserOnlineStatusReq req);
}
