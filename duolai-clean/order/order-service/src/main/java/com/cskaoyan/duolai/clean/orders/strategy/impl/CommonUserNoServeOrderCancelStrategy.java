package com.cskaoyan.duolai.clean.orders.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.cskaoyan.duolai.clean.common.expcetions.ForbiddenOperationException;
import com.cskaoyan.duolai.clean.orders.client.OrderDispatchApi;
import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusChangeEventEnum;
import com.cskaoyan.duolai.clean.orders.job.OrdersHandler;
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
import java.time.temporal.ChronoUnit;

/**
 * 普通用户对待服务状态订单取消
 **/
@Component("1:NO_SERVE")
public class CommonUserNoServeOrderCancelStrategy implements OrderCancelStrategy {
    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private IOrdersCanceledService ordersCanceledService;
    @Resource
    private IOrdersRefundService ordersRefundService;
    @Resource
    private OrdersHandler ordersHandler;

    @Resource
    private OrderDispatchApi ordersSeizeApi;

    /**
     * 订单取消
     *
     * @param orderCancelDTO 订单取消模型
     */
    @Override
    @Transactional
    public void cancel(OrderCancelDTO orderCancelDTO) {
        // 校验是否为本人操作
        if (ObjectUtil.notEqual(orderCancelDTO.getUserId(), orderCancelDTO.getCurrentUserId())) {
            throw new ForbiddenOperationException("非本人操作");
        }

        //2.校验当前时间距预约时间是否满120分钟
        long between = LocalDateTimeUtil.between(LocalDateTime.now(), orderCancelDTO.getServeStartTime(), ChronoUnit.MINUTES);
        if (between < 120) {
            throw new ForbiddenOperationException("离预约时间不足120分钟，请联系客服取消");
        }

        //3.状态机更新订单状态
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .refundStatus(RefundStatusEnum.SENDING.getCode())
                .cancellerId(orderCancelDTO.getCurrentUserId())
                .cancelerName(orderCancelDTO.getCurrentUserName())
                .cancellerType(orderCancelDTO.getCurrentUserType())
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        //3.保存订单取消记录
        OrdersCanceledDO ordersCanceledDO = BeanUtil.toBean(orderSnapshotDTO, OrdersCanceledDO.class);
        ordersCanceledDO.setId(orderCancelDTO.getId());
        ordersCanceledService.save(ordersCanceledDO);

        //4.订单状态变更
        orderStateMachine.changeStatus(orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CLOSE_NO_SERVE_ORDER, orderSnapshotDTO);

        //5.取消服务单
        ordersSeizeApi.noServeCancelByUserAndOperation(orderCancelDTO.getId());

        //6.存入退款表，定时任务扫描进行退款
        OrdersRefundDO ordersRefundDO = new OrdersRefundDO();
        ordersRefundDO.setId(orderCancelDTO.getId());
        ordersRefundDO.setTradingOrderNo(orderCancelDTO.getTradingOrderNo());
        ordersRefundDO.setRealPayAmount(orderCancelDTO.getRealPayAmount());
        ordersRefundService.save(ordersRefundDO);
    }
}
