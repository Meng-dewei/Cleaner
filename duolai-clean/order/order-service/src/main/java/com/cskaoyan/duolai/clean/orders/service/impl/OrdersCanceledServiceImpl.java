package com.cskaoyan.duolai.clean.orders.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.orders.model.mapper.OrdersCanceledMapper;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersCanceledDO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCanceledService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  订单取消服务实现类
 * </p>
 */
@Service
public class OrdersCanceledServiceImpl extends ServiceImpl<OrdersCanceledMapper, OrdersCanceledDO> implements IOrdersCanceledService {

}
