package com.cskaoyan.duolai.clean.orders.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusChangeEventEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersCanceledDO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCanceledService;
import com.cskaoyan.duolai.clean.orders.strategy.OrderCancelStrategy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 系统对待支付状态超时订单取消
 **/
@Component("0:NO_PAY")
public class SystemNoPayOrderCancelStrategy implements OrderCancelStrategy {
    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private IOrdersCanceledService ordersCanceledService;

    /**
     * 订单取消
     *
     * @param orderCancelDTO 订单取消模型
     */
    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        //构建订单快照更新模型
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancellerType(orderCancelDTO.getCurrentUserType())
                .cancelTime(LocalDateTime.now())
                .build();

        //保存订单取消记录
        OrdersCanceledDO ordersCanceledDO = BeanUtil.toBean(orderSnapshotDTO, OrdersCanceledDO.class);
        ordersCanceledDO.setId(orderCancelDTO.getId());
        ordersCanceledService.save(ordersCanceledDO);

        //订单状态变更
        orderStateMachine.changeStatus(orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CANCEL, orderSnapshotDTO);
    }
}
