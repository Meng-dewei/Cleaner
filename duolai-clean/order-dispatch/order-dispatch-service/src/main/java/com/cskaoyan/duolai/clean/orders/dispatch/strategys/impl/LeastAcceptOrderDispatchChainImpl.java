package com.cskaoyan.duolai.clean.orders.dispatch.strategys.impl;

import com.cskaoyan.duolai.clean.orders.dispatch.strategys.annotations.DispatchStrategy;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.DispatchStrategyEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.IDispatchRule;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.impl.AcceptNumDispatchRule;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.impl.DistanceDispatchRule;
import org.springframework.stereotype.Component;

/**
 * 最少接单优先策略
 * 算规则执行顺序如下：
 * 按最少接单数规则->按距离计算规则
 */
@Component("leastAcceptOrderDispatchStrategy")
@DispatchStrategy(DispatchStrategyEnum.LEAST_ACCEPT_ORDER)
public class LeastAcceptOrderDispatchChainImpl extends AbstractDispatchChainImpl {
    @Override
    protected IDispatchRule getRules() {
        // 按评分计算规则,评分越高优先级越高
        IDispatchRule distanceDispatchRule = new DistanceDispatchRule(null);
        // 按最少接单数规则,接单数量越少优先级越高
        IDispatchRule acceptNumDispatchRule = new AcceptNumDispatchRule(distanceDispatchRule);
        return acceptNumDispatchRule;
    }
}
