package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.utils.CollUtils;
import com.cskaoyan.duolai.clean.common.utils.DateUtils;
import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.converter.OrderServeConverter;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersServeMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderServeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum.SERVING;
import static com.cskaoyan.duolai.clean.orders.dispatch.enums.ServeStatusEnum.NO_ALLOCATION;
import static com.cskaoyan.duolai.clean.orders.dispatch.enums.ServeStatusEnum.NO_SERVED;


/**
 * <p>
 * 服务任务 服务实现类
 * </p>
 *
 */
@Service
public class OrderServeServiceImpl extends ServiceImpl<OrdersServeMapper, OrderServeDO> implements IOrderServeService {


    @Resource
    OrderServeConverter orderServeConverter;

    @Override
    public List<Long> countServeTimes(Long serveProviderId) {

        List<OrderServeDO> list = lambdaQuery()
                .ge(OrderServeDO::getId, 0)
                .eq(OrderServeDO::getServeProviderId, serveProviderId)
                .in(OrderServeDO::getServeStatus, Arrays.asList(NO_SERVED.getStatus(), SERVING.getStatus()))
                .select(OrderServeDO::getServeStartTime)
                .list();
        if(CollUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 将预约时间转换成指定格式
        return list.stream()
                .map(ordersServe -> DateUtils.getFormatDate(ordersServe.getServeStartTime(), "yyyMMddHH"))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Integer countNoServedNum(Long serveProviderId) {
        return lambdaQuery()
                .ge(OrderServeDO::getId, 0)
                .eq(OrderServeDO::getServeProviderId, serveProviderId)
                .in(OrderServeDO::getServeStatus, Arrays.asList( NO_SERVED.getStatus(), SERVING.getStatus()))
                .count().intValue();
    }

    @Override
    public List<OrderServeDTO> findByOrderId(Long id) {
       LambdaQueryWrapper<OrderServeDO> wrapper = Wrappers.lambdaQuery(OrderServeDO.class)
                .eq(OrderServeDO::getOrdersId, id)
                .ge(OrderServeDO::getServeProviderId, 0);
        List<OrderServeDO> orderServeDOs = baseMapper.selectList(wrapper);
        return orderServeConverter.ordersServeDOsToOrdersServeDTOs(orderServeDOs);
    }


}
