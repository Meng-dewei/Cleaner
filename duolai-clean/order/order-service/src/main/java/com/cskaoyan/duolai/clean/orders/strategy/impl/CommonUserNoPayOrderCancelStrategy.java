package com.cskaoyan.duolai.clean.orders.strategy.impl;

import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
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
 * 普通用户对待支付状态订单取消
 **/
@Component("1:NO_PAY")
public class CommonUserNoPayOrderCancelStrategy implements OrderCancelStrategy {
    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private IOrdersCanceledService ordersCanceledService;

    @Resource
    OrderConverter orderConverter;

    /**
     * 订单取消
     *
     * @param orderCancelDTO 订单取消模型
     */
    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {

        //2.构建订单快照更新模型
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .cancellerId(orderCancelDTO.getCurrentUserId())
                .cancelerName(orderCancelDTO.getCurrentUserName())
                .cancellerType(orderCancelDTO.getCurrentUserType())
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        //3.保存订单取消记录
        OrdersCanceledDO ordersCanceledDO = orderConverter.orderSnapshotDTOtoCanceledDO(orderSnapshotDTO);
        ordersCanceledDO.setId(orderCancelDTO.getId());
        ordersCanceledService.save(ordersCanceledDO);

        //4.订单状态变更
        orderStateMachine.changeStatus( orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CANCEL, orderSnapshotDTO);
    }
}
