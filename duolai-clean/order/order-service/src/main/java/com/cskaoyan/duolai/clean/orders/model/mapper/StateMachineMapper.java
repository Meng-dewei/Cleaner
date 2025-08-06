package com.cskaoyan.duolai.clean.orders.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cskaoyan.duolai.clean.orders.model.entity.OrderStatePersisterDO;

/**
 * 状态持久化数据层
 **/
public interface StateMachineMapper extends BaseMapper<OrderStatePersisterDO> {
}
