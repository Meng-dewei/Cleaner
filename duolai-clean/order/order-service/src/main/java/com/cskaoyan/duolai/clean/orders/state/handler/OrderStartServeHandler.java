package com.cskaoyan.duolai.clean.orders.state.handler;

import cn.hutool.db.DbRuntimeException;
import com.cskaoyan.duolai.clean.orders.enums.StatusChangeEvent;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCommonService;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.dto.OrderUpdateDTO;
import com.cskaoyan.duolai.clean.orders.state.core.StatusChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单开始服务处理器
 **/
@Slf4j
@Component("order_start_serve")
public class OrderStartServeHandler implements StatusChangeHandler<OrderSnapshotDTO> {
    @Resource
    private IOrdersCommonService ordersService;

    /**
     * 订单开始服务处理逻辑
     *
     * @param bizId   业务id
     * @param bizSnapshot 快照
     */
    @Override
    public void handler(String bizId, OrderSnapshotDTO bizSnapshot) {
        log.info("订单开始服务事件处理逻辑开始，订单号：{}", bizId);
        // 修改订单状态
        OrderUpdateDTO orderUpdateDTO = OrderUpdateDTO.builder().id(Long.valueOf(bizId))
                .originStatus(OrderStatusEnum.NO_SERVE.getStatus())
                .targetStatus(OrderStatusEnum.SERVING.getStatus())
                .build();
        int result = ordersService.updateStatus(orderUpdateDTO);
        if (result <= 0) {
            throw new DbRuntimeException("订单开始服务事件处理失败");
        }
    }
}
