package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.utils.NumberUtils;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.ServeProviderSyncMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.ServeProviderSyncDO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IServeProviderSyncService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 机构服务端更新服务时间 服务实现类
 * </p>
 */
@Service
public class ServeProviderSyncServiceImpl extends ServiceImpl<ServeProviderSyncMapper, ServeProviderSyncDO> implements IServeProviderSyncService {

    @Resource
    private IOrderServeService ordersServeService;

    @Override
    public void countServeTimesAndAcceptanceNum(Long id) {
        ServeProviderSyncDO serveProviderSyncDO = new ServeProviderSyncDO();
        serveProviderSyncDO.setId(id);
        List<Long> serveTimes = ordersServeService.countServeTimes(id);
        serveProviderSyncDO.setServeTimes(serveTimes);
        Integer acceptanceNum = ordersServeService.countNoServedNum(id);
        serveProviderSyncDO.setAcceptanceNum(NumberUtils.null2Zero(acceptanceNum));
        this.saveOrUpdate(serveProviderSyncDO);
    }
}
