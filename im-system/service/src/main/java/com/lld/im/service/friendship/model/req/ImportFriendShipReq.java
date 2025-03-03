package com.lld.im.service.friendship.model.req;

import com.lld.im.common.enums.FriendShipStatusEnum;
import com.lld.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @Author: 萱子王
 * @CreateTime: 2025-03-02
 * @Description: 导入好友关系链的请求实体类
 * @Version: 1.0
 */

@Data
public class ImportFriendShipReq extends RequestBase {

    // 要导入的好友关系中的from者
    @NotBlank(message = "fromId不能为空！")
    private String fromId;

    // 要导入的from者的所有好友列表
    private List<ImportFriendDto> friendItem;

    @Data
    public static class ImportFriendDto{

        // 好友关系链中的to者
        private String toId;

        // 好友关系链的备注
        private String remark;

        // 添加来源
        private String addSource;

        // 好友状态
        private Integer status= FriendShipStatusEnum.FRIEND_STATUS_NO_FRIEND.getCode();

        // 拉黑状态
        private Integer black=FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode();
    }

}
