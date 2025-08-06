package com.cskaoyan.duolai.clean.orders.state.core;


public interface StateMachineSnapshot {

    /**
     * 返回快照id
     * @return
     */
    String getSnapshotId();
    /**
     * 返回快照状态
     * @return
     */
    Integer getSnapshotStatus();
    /**
     * 设置快照id
     */
    void setSnapshotId(String snapshotId);
    /**
     * 设置快照状态
     */
    void setSnapshotStatus(Integer snapshotStatus);
}
