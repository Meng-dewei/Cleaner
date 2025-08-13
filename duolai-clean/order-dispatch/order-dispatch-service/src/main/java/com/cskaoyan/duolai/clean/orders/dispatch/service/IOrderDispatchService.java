package com.cskaoyan.duolai.clean.orders.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.DispatchStrategyEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderDispatchDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderDispatchDTO;

import java.util.List;

/**
 * <p>
 * 订单表 服务类
 * </p>
 */
public interface IOrderDispatchService extends IService<OrderDispatchDO> {



    /**
     * 派单接口
     * @param orderDispatchDO
     */
    void dispatch(OrderDispatchDO orderDispatchDO);




    /**
     *  查询符合条件的服务人员或机构
     * @param cityCode 城市编码
     * @param serveItemId 服务项id
     * @param maxDistance 派单最大限制距离
     * @param serveTime 服务时间格式yyyyMMddHH 数字格式
     * @param dispatchStrategyEnum 派单策略，距离优先策略、最少接单策略、
     * @param lon
     * @param lat
     * @param limit
     * @return
     */
    List<ServeProviderDispatchDTO> searchDispatchInfo(String cityCode, long serveItemId, double maxDistance, Long serveTime, DispatchStrategyEnum dispatchStrategyEnum, Double lon, Double lat, int limit);

}
