package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.utils.DateUtils;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.BreachTypeEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.BreachRecordMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.BreachRecordDO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IBreachRecordService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 违约记录 服务实现类
 * </p>
 */
@Service
public class BreachRecordServiceImpl extends ServiceImpl<BreachRecordMapper, BreachRecordDO> implements IBreachRecordService {



    @Override
    public void add(BreachRecordDO breachRecordDO) {
        breachRecordDO.setBreachDay(DateUtils.getDay());
        breachRecordDO.setBreachTime(DateUtils.now());
        baseMapper.insert(breachRecordDO);
    }


    @Override
    public int count(Long serveProviderId, BreachTypeEnum breachTypeEnum, int breachDay) {
        return lambdaQuery().eq(BreachRecordDO::getServeProviderId, serveProviderId)
                .eq(BreachRecordDO::getBehaviorType, breachTypeEnum.getType())
                .eq(BreachRecordDO::getBreachDay, breachDay)
                .count().intValue();
    }
}
