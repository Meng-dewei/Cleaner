package com.cskaoyan.duolai.clean.orders.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import com.cskaoyan.duolai.clean.orders.client.OrderDispatchApi;
import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
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

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 运营端派单状态取消订单
 **/
@Component("3:DISPATCHING")
public class OperationDispatchingOrderCancelStrategy implements OrderCancelStrategy {
    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private IOrdersCanceledService ordersCanceledService;

    @Resource
    private IOrdersRefundService ordersRefundService;

    @Resource
    private OrderDispatchApi ordersSeizeApi;

    /**
     * 订单取消
     *
     * @param orderCancelDTO 订单取消模型
     */
    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {
        //1.构建订单快照
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .refundStatus(RefundStatusEnum.SENDING.getCode())
                .cancellerId(orderCancelDTO.getCurrentUserId())
                .cancelerName(orderCancelDTO.getCurrentUserName())
                .cancellerType(orderCancelDTO.getCurrentUserType())
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        //2.保存订单取消记录
        OrdersCanceledDO ordersCanceledDO = BeanUtil.toBean(orderSnapshotDTO, OrdersCanceledDO.class);
        ordersCanceledDO.setId(orderCancelDTO.getId());
        ordersCanceledService.save(ordersCanceledDO);

        //3.订单状态变更
        orderStateMachine.changeStatus(orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CLOSE_DISPATCHING_ORDER, orderSnapshotDTO);

        //5.取消抢派单
        ordersSeizeApi.clearSeizeDispatchPool(orderCancelDTO.getId(), orderCancelDTO.getCityCode());

        //6.存入退款表，定时任务扫描进行退款
        OrdersRefundDO ordersRefundDO = new OrdersRefundDO();
        ordersRefundDO.setId(orderCancelDTO.getId());
        ordersRefundDO.setTradingOrderNo(orderCancelDTO.getTradingOrderNo());
        ordersRefundDO.setRealPayAmount(orderCancelDTO.getRealPayAmount());
        ordersRefundService.save(ordersRefundDO);
    }
}
