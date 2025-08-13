package com.cskaoyan.duolai.clean.orders.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.ServeProviderSyncDO;

/**
 * <p>
 * 机构服务端更新服务时间 服务类
 * </p>
 */
public interface IServeProviderSyncService extends IService<ServeProviderSyncDO> {


    /**
     * 统计用户前服务时间列表和接单数量
     *
     * @param id 服务人员或机构id
     */
    void countServeTimesAndAcceptanceNum(Long id);




}
