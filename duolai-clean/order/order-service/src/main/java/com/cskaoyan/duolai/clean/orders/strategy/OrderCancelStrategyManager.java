package com.cskaoyan.duolai.clean.orders.strategy;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.cskaoyan.duolai.clean.common.expcetions.ForbiddenOperationException;
import com.cskaoyan.duolai.clean.market.request.CouponUseBackParam;
import com.cskaoyan.duolai.clean.orders.client.CouponApi;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.service.IOrdersManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class OrderCancelStrategyManager {
    @Resource
    private IOrdersManagerService ordersManagerService;

    //key格式：userType+":"+orderStatusEnum，例：1：NO_PAY
    private final Map<String, OrderCancelStrategy> strategyMap = new HashMap<>();

    @Resource
    CouponApi couponApi;


    @PostConstruct
    public void init() {
        Map<String, OrderCancelStrategy> strategies = SpringUtil.getBeansOfType(OrderCancelStrategy.class);
        strategyMap.putAll(strategies);
        log.debug("订单取消策略类初始化到map完成！");
    }

    /**
     * 获取策略实现类
     *
     * @param userType    用户类型
     * @param orderStatus 订单状态
     * @return 策略实现类
     */
    public OrderCancelStrategy getStrategy(Integer userType, Integer orderStatus) {
        String key = userType + ":" + OrderStatusEnum.codeOf(orderStatus).toString();
        return strategyMap.get(key);
    }

    /**
     * 订单取消
     *
     * @param orderCancelDTO 订单取消模型
     */
    public void cancel(OrderCancelDTO orderCancelDTO, Integer currentStatus, BigDecimal discountAmount) {
        if (currentStatus.equals(OrderStatusEnum.CANCELED.getStatus())
                || currentStatus.equals(OrderStatusEnum.CLOSED.getStatus()) ) {
            return;
        }

        // 选择取消订单的策略
        OrderCancelStrategy strategy = getStrategy(orderCancelDTO.getCurrentUserType(), currentStatus);
        if (ObjectUtil.isEmpty(strategy)) {
            throw new ForbiddenOperationException("不被许可的操作");
        }

        if (discountAmount.doubleValue() > 0) {
            // 使用了优惠卷，那么需要退回优惠卷
            CouponUseBackParam couponUseBackCommand = new CouponUseBackParam();
            couponUseBackCommand.setUserId(orderCancelDTO.getUserId());
            couponUseBackCommand.setOrdersId(orderCancelDTO.getId());

            couponApi.useBack(couponUseBackCommand);
        }
        strategy.cancel(orderCancelDTO);
    }

}
