package com.cskaoyan.duolai.clean.orders.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersRefundDO;

import java.util.List;

/**
 * <p>
 * 订单退款表 服务类
 * </p>
 */
public interface IOrdersRefundService extends IService<OrdersRefundDO> {

    /**
     * 查询指定数量的退款订单
     *
     * @param count 数量
     */
    List<OrdersRefundDO> queryRefundOrderListByCount(Integer count);
}
