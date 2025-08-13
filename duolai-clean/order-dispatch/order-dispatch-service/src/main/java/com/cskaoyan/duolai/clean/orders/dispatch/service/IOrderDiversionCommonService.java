package com.cskaoyan.duolai.clean.orders.dispatch.service;


import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;

/**
 * 订单分流
 */
public interface IOrderDiversionCommonService {

    /**
     * 订单分流,所有订单均可抢单
     *
     */
    void diversion(OrderParam orderParam);

}
