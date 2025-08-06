package com.cskaoyan.duolai.clean.orders.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.dto.OrderUpdateDTO;

/**
 * <p>
 * 订单表 服务类
 * </p>
 */
public interface IOrdersCommonService extends IService<OrdersDO> {

    Integer updateStatus(OrderUpdateDTO orderUpdateStatusReqDTO);
}
