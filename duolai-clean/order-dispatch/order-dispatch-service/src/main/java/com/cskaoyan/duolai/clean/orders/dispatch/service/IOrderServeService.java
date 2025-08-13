package com.cskaoyan.duolai.clean.orders.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderServeDO;

import java.util.List;

/**
 * <p>
 * 服务任务 服务类
 * </p>
 */
public interface IOrderServeService extends IService<OrderServeDO> {

    /**
     * 统计当前服务单数（包含待接单、待开始、待分配
     * @param serveProviderId
     * @return
     */
    List<Long> countServeTimes(Long serveProviderId);

    /**
     * 统计当前订单未完成的服务数量
     * @param serveProviderId
     * @return
     */
    Integer countNoServedNum(Long serveProviderId);

    List<OrderServeDTO> findByOrderId(Long id);

}
