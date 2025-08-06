package com.cskaoyan.duolai.clean.orders.state.persist;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
import com.cskaoyan.duolai.clean.orders.enums.StatusDefine;
import com.cskaoyan.duolai.clean.orders.model.mapper.StateMachineMapper;
import com.cskaoyan.duolai.clean.orders.model.entity.OrderStatePersisterDO;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


/**
 * 状态机持久化实现
 *
 *  “”
 **/
@Component
@Primary
public class StateMachinePersisterImpl implements StateMachinePersister {
    /**
     * 状态机数据层处理类
     */
    private final StateMachineMapper stateMachineMapper;

    /**
     * 构造器
     *
     * @param stateMachineMapper 状态机数据层处理类
     */
    public StateMachinePersisterImpl(StateMachineMapper stateMachineMapper) {
        this.stateMachineMapper = stateMachineMapper;
    }

    /**
     * 业务数据状态初始化
     *
     * @param stateMachineName 状态机名称
     * @param bizId            业务id
     * @param statusDefine     当前状态
     */
    @Override
    public void init(String stateMachineName, String bizId, StatusDefine statusDefine) {
        String code = statusDefine.getCode();

        OrderStatePersisterDO orderStatePersisterDO = OrderStatePersisterDO.builder()
                .stateMachineName(stateMachineName)
                .bizId(bizId).state(code).build();
        stateMachineMapper.insert(orderStatePersisterDO);
    }

    /**
     * 业务数据状态持久化
     *
     * @param stateMachineName 状态机名称
     * @param bizId            业务id
     * @param statusDefine     当前状态
     */
    @Override
    public void persist(String stateMachineName, String bizId, StatusDefine statusDefine) {
        String code = statusDefine.getCode();

        LambdaUpdateWrapper<OrderStatePersisterDO> updateWrapper = Wrappers.<OrderStatePersisterDO>lambdaUpdate()
                .eq(OrderStatePersisterDO::getStateMachineName, stateMachineName)
                .eq(OrderStatePersisterDO::getBizId, bizId)
                .set(OrderStatePersisterDO::getState, code);
        stateMachineMapper.update(null, updateWrapper);
    }

    /**
     * 查询业务数据当前持久化状态
     *
     * @param stateMachineName 状态机名称
     * @param bizId            业务id
     * @return 当前持久化状态代码
     */
    @Override
    public String getCurrentState(String stateMachineName, String bizId) {
        LambdaQueryWrapper<OrderStatePersisterDO> queryWrapper = Wrappers.<OrderStatePersisterDO>lambdaQuery()
                .eq(OrderStatePersisterDO::getStateMachineName, stateMachineName)
                .eq(OrderStatePersisterDO::getBizId, bizId)
                .select(OrderStatePersisterDO::getState);
        OrderStatePersisterDO orderStatePersisterDO = stateMachineMapper.selectOne(queryWrapper);
        return ObjectUtils.get(orderStatePersisterDO, OrderStatePersisterDO::getState);
    }

}
