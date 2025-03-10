package com.lld.im.service.group.service;

import com.lld.im.common.ResponseVO;
import com.lld.im.service.group.dao.ImGroupEntity;
import com.lld.im.service.group.model.req.CreateGroupReq;
import com.lld.im.service.group.model.req.GetGroupInfoReq;
import com.lld.im.service.group.model.req.ImportGroupReq;
import com.lld.im.service.group.model.req.UpdateGroupReq;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-09
 * @Description: 群组模块的业务逻辑接口
 * @Version: 1.0
 */

public interface ImGroupService {

    /***
     * 导入群组(1个)
     * @param importGroupReq
     * @return
     */
    public ResponseVO importGroup(ImportGroupReq importGroupReq);


    /***
     * 根据groupId和appId获取群聊(内部调用)
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);


    /***
     *  创建群组(这里默认如果要根据群id创建一个已经创建过且删除的群聊是不行的，即使是相同的群聊再次创建也必须是不同的id)
     * @param req
     * @return
     */
    public ResponseVO createGroup(CreateGroupReq req);


    /***
     * 修改群组信息(根据操作人身份鉴权版)
     * @param req
     * @return
     */
    public ResponseVO updateGroupInfo(UpdateGroupReq req);


    /***
     * 获取群组信息(外部接口调用)
     * @param req
     * @return
     */
    public ResponseVO getGroupInfo(GetGroupInfoReq req);
}
