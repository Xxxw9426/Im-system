package com.lld.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.codec.pack.group.AddGroupMemberPack;
import com.lld.im.codec.pack.group.RemoveGroupMemberPack;
import com.lld.im.codec.pack.group.UpdateGroupMemberPack;
import com.lld.im.common.ClientType;
import com.lld.im.common.enums.command.Command;
import com.lld.im.common.enums.command.GroupEventCommand;
import com.lld.im.common.model.ClientInfo;
import com.lld.im.service.group.model.req.GroupMemberDto;
import com.lld.im.service.group.service.ImGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-29
 * @Description: 处理群组业务逻辑后向mq中发送tpc通知消息的实体类
 * @Version: 1.0
 */

@Component
public class GroupMessageProducer {

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImGroupMemberService imGroupMemberService;


    /***
     * 发送消息的方法
     * @param userId         操作的用户id
     * @param command        指令
     * @param data           数据包
     * @param clientInfo     发送过程中发送给的用户的所需信息
     */
    public void producer(String userId, Command command, Object data, ClientInfo clientInfo) {

        JSONObject o = (JSONObject) JSONObject.toJSON(data);
        String groupId = o.getString("groupId");
        // 首先获取当前群聊中的所有群成员id
        List<String> groupMemberId = imGroupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());
        // 针对一些特殊逻辑进行前置处理

        // TODO 如果是加入群成员操作的话,只需要给管理员和被加入人发送tcp通知
        if(command.equals(GroupEventCommand.ADDED_MEMBER)) {
            // 获取到群组的所有管理员
            List<GroupMemberDto> groupManager = imGroupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            // 获取到所有加入群组的用户
            AddGroupMemberPack addGroupMemberPack = o.toJavaObject(AddGroupMemberPack.class);
            List<String> members = addGroupMemberPack.getMembers();
            for(GroupMemberDto manager : groupManager) {
                // 如果当前遍历到的用户就是操作人，则发送给操作人除了当前端以外的所有在线端
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode() && manager.getMemberId().equals(userId)){
                    messageProducer.sendToUserExceptClient(manager.getMemberId(),command,data,clientInfo);

                // 如果不是操作人的话，则发送给所有在线的端
                }else{
                    messageProducer.sendToUser(manager.getMemberId(),command,data,clientInfo.getAppId());
                }
            }
            for(String member : members) {
                // 如果当前遍历到的用户就是操作人，则发送给操作人除了当前端以外的所有在线端
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)){
                    messageProducer.sendToUserExceptClient(member,command,data,clientInfo);

                // 如果不是操作人的话，则发送给所有在线的端
                }else{
                    messageProducer.sendToUser(member,command,data,clientInfo.getAppId());
                }
            }

        // TODO 如果是踢人出群操作的话,需要发送给群内的所有群成员和被踢人
        } else if(command.equals(GroupEventCommand.DELETED_MEMBER)) {
            // 获取到被踢出群聊的用户
            RemoveGroupMemberPack removeGroupMemberPack = o.toJavaObject(RemoveGroupMemberPack.class);
            String member = removeGroupMemberPack.getMember();
            groupMemberId.add(member);
            for(String mem: groupMemberId) {
                // 如果当前遍历到的用户就是操作人，则发送给操作人除了当前端以外的所有在线端
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode() && mem.equals(userId)){
                    messageProducer.sendToUserExceptClient(mem,command,data,clientInfo);

                // 如果不是操作人的话，则发送给所有在线的端
                }else{
                    messageProducer.sendToUser(mem,command,data,clientInfo.getAppId());
                }
            }

        // TODO 如果是修改群成员操作的话，则只需要发送给群管理员和操作人本身即可
        } else if(command.equals(GroupEventCommand.UPDATED_MEMBER)) {
            // 获取到修改的群成员
            UpdateGroupMemberPack updateGroupMemberPack = o.toJavaObject(UpdateGroupMemberPack.class);
            String memberId = updateGroupMemberPack.getMemberId();
            // 获取到群组的所有管理员
            List<GroupMemberDto> groupManager = imGroupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            GroupMemberDto dto = new GroupMemberDto();
            dto.setMemberId(memberId);
            groupManager.add(dto);
            for (GroupMemberDto member : groupManager) {
                if(clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)){
                    // 如果当前遍历到的用户就是操作人，则发送给操作人除了当前端以外的所有在线端
                    messageProducer.sendToUserExceptClient(member.getMemberId(),command,data,clientInfo);

                // 如果不是操作人的话，则发送给所有在线的端
                }else{
                    messageProducer.sendToUser(member.getMemberId(),command,data,clientInfo.getAppId());
                }
            }

        } else {
            for(String memberId : groupMemberId) {
                // 遍历到每一个群内用户并且发送tcp通知
                // 这里的webAPI端其实可以理解为是APP管理员发送的请求
                // 如果是操作人的话
                if(clientInfo.getClientType()!=null && clientInfo.getClientType()!= ClientType.WEBAPI.getCode() && memberId.equals(userId)) {
                    // 发送给操作人除了当前端以外的所有在线端
                    messageProducer.sendToUserExceptClient(memberId,command, data,clientInfo);

                    // 如果不是操作人的话
                } else {
                    // 发送给所有在线的端
                    messageProducer.sendToUser(memberId,command, data,clientInfo.getAppId());
                }
            }
        }

    }
}
