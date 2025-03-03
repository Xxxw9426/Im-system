package com.lld.im.service.user.model.resp;

import com.lld.im.service.user.dao.ImUserDataEntity;
import lombok.Data;

import java.util.List;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class GetUserInfoResp {

    // 返回的查询到的所有用户资料
    private List<ImUserDataEntity> userDataItem;

    // 返回查询失败的用户id
    private List<String> failUser;


}
