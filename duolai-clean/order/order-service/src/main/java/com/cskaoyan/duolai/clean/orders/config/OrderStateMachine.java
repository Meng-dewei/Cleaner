package com.cskaoyan.duolai.clean.orders.config;

import cn.hutool.core.util.*;
import com.cskaoyan.duolai.clean.orders.constants.RedisConstants;
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
import com.cskaoyan.duolai.clean.orders.converter.SnapshotConverter;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.service.BizSnapshotService;
import com.cskaoyan.duolai.clean.orders.state.AbstractStateMachine;
import com.cskaoyan.duolai.clean.orders.state.persist.StateMachinePersister;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 订单状态机
 *
 **/
@Component
public class OrderStateMachine extends AbstractStateMachine {

    public OrderStateMachine(StateMachinePersister stateMachinePersister, BizSnapshotService bizSnapshotService
            , RedissonClient redissonClient, SnapshotConverter snapshotConverter) {
        super(stateMachinePersister, bizSnapshotService, redissonClient, snapshotConverter);
    }

    /**
     * 设置状态机名称
     *
     * @return 状态机名称
     */
    @Override
    protected String getName() {
        return "order";
    }


    /**
     * 设置状态机初始状态
     *
     * @return 状态机初始状态
     */
    @Override
    protected OrderStatusEnum getInitState() {
        return OrderStatusEnum.NO_PAY;
    }

}
