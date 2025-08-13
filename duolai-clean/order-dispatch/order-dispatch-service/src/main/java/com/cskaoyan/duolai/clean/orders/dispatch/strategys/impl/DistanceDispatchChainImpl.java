package com.cskaoyan.duolai.clean.orders.dispatch.strategys.impl;

import com.cskaoyan.duolai.clean.orders.dispatch.strategys.annotations.DispatchStrategy;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.DispatchStrategyEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.IDispatchRule;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.impl.AcceptNumDispatchRule;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.impl.DistanceDispatchRule;
import org.springframework.stereotype.Component;

/**
 * 距离优先
 * 先根据距离计算得分，算规则执行顺序如下：
 * 按距离计算->按最少接单数计算
 */
@Component("distanceDispatchStrategy")
@DispatchStrategy(DispatchStrategyEnum.DISTANCE)
public class DistanceDispatchChainImpl extends AbstractDispatchChainImpl {
    @Override
    protected IDispatchRule getRules() {
        // 最少接单规则
        IDispatchRule acceptNumDispatchRule = new AcceptNumDispatchRule(null);
        // 距离优先规则
        IDispatchRule distanceDispatchRule = new DistanceDispatchRule(acceptNumDispatchRule);
        return distanceDispatchRule;
    }

}
