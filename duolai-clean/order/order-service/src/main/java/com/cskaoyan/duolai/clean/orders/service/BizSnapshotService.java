package com.cskaoyan.duolai.clean.orders.service;

import com.cskaoyan.duolai.clean.orders.enums.StatusDefine;

/**
 * 业务数据服务层
 **/
public interface BizSnapshotService {
    /**
     * 新增业务快照
     *
     * @param stateMachineName 状态机名称
     * @param bizId            业务id
     * @param statusDefine     状态
     * @param bizSnapshot      业务快照
     */
    void save(String stateMachineName, String bizId, StatusDefine statusDefine, String bizSnapshot);

    /**
     * 根据业务id和状态查询最新业务快照
     *
     * @param stateMachineName 状态机名称
     * @param bizId            业务id
     * @param state            状态代码
     * @return 业务快照
     */
    String findLastSnapshotByBizIdAndState(String stateMachineName, String bizId, String state);
}
