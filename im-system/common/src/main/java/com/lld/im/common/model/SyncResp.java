package com.lld.im.common.model;

import lombok.Data;

import java.util.List;

/**
 * @author: Chackylee
 * @description: 增量同步时服务端的响应
 **/
@Data
public class SyncResp<T> {

    private Long maxSequence;         // 本次拉取结果中最大的seq

    private boolean isCompleted;      // 是否拉取成功

    private List<T> dataList;         // 数据集

}
