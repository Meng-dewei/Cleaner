package com.cskaoyan.duolai.clean.orders.strategy.impl;

import com.cskaoyan.duolai.clean.orders.client.OrderDispatchApi;
import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusChangeEventEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersCanceledDO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersRefundDO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCanceledService;
import com.cskaoyan.duolai.clean.orders.service.IOrdersRefundService;
import com.cskaoyan.duolai.clean.orders.strategy.OrderCancelStrategy;
import com.cskaoyan.duolai.clean.pay.enums.RefundStatusEnum;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 普通用户派单状态取消订单
 **/
@Component("1:DISPATCHING")
public class CommonUserDispatchingOrderCancelStrategy implements OrderCancelStrategy {
    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private IOrdersCanceledService ordersCanceledService;

    @Resource
    private IOrdersRefundService ordersRefundService;

    @Resource
    OrderConverter orderConverter;

    @Resource
    OrderDispatchApi ordersSeizeApi;

    /**
     * 订单取消
     *
     * @param orderCancelDTO 订单取消模型
     */
    @Override
    @Transactional
    public void cancel(OrderCancelDTO orderCancelDTO) {
        //2.构建订单快照更新模型
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .refundStatus(RefundStatusEnum.SENDING.getCode())
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
        orderStateMachine.changeStatus(orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CLOSE_DISPATCHING_ORDER, orderSnapshotDTO);


        //5.存入退款表，定时任务扫描进行退款
        OrdersRefundDO ordersRefundDO = new OrdersRefundDO();
        ordersRefundDO.setId(orderCancelDTO.getId());
        ordersRefundDO.setTradingOrderNo(orderCancelDTO.getTradingOrderNo());
        ordersRefundDO.setRealPayAmount(orderCancelDTO.getRealPayAmount());
        ordersRefundService.save(ordersRefundDO);

        //6.清理抢派单池
        ordersSeizeApi.clearSeizeDispatchPool(orderCancelDTO.getId(), orderCancelDTO.getCityCode());
    }
}
