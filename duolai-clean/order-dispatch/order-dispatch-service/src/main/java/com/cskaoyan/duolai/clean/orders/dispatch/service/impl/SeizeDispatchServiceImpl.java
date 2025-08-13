package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;

import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.orders.dispatch.config.RedissonCancelSeizeOrderLuaHandler;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersDispatchMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersSeizeMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.service.ISeizeDispatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class SeizeDispatchServiceImpl implements ISeizeDispatchService {



    @Resource
    private OrdersSeizeMapper ordersSeizeMapper;
    @Resource
    private OrdersDispatchMapper ordersDispatchMapper;

    @Resource
    RedissonCancelSeizeOrderLuaHandler redissonCancelSeizeOrderLuaHandler;


    @Override
    @Transactional
    public void clearSeizeDispatchPool(String cityCode, Long id) {
        Long result = redissonCancelSeizeOrderLuaHandler.cancelOrderSeize(id, cityCode);
        if (result == -1) {
            // 重复取消
            return;
        }

        if (result == -2) {
            // 库存不足
            throw new CommonException("已经抢单成功，请稍后重试");
        }

        // 删除派单池，抢单池中的数据，同时，因为有canal同步，所以es中的数据也会被删除
        ordersDispatchMapper.deleteById(id);
        ordersSeizeMapper.deleteById(id);
    }

}
