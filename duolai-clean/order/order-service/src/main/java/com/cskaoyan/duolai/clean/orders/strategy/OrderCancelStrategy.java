package com.cskaoyan.duolai.clean.orders.strategy;


import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;

/**
 * 订单取消策略类
 **/
public interface OrderCancelStrategy {
    /**
     * 订单取消
     *
     * @param orderCancelDTO 订单取消模型
     */
    void cancel(OrderCancelDTO orderCancelDTO);
}
