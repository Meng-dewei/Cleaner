package com.cskaoyan.duolai.clean.orders.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.orders.model.mapper.OrdersRefundMapper;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersRefundDO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersRefundService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 订单退款表 服务实现类
 * </p>
 */
@Service
public class OrdersRefundServiceImpl extends ServiceImpl<OrdersRefundMapper, OrdersRefundDO> implements IOrdersRefundService {

    /**
     * 查询指定数量的退款订单
     *
     * @param count 数量
     */
    @Override
    public List<OrdersRefundDO> queryRefundOrderListByCount(Integer count) {
        LambdaQueryWrapper<OrdersRefundDO> queryWrapper = Wrappers.<OrdersRefundDO>lambdaQuery()
                .orderByAsc(OrdersRefundDO::getCreateTime)
                .last("limit " + count);
        return baseMapper.selectList(queryWrapper);
    }
}
