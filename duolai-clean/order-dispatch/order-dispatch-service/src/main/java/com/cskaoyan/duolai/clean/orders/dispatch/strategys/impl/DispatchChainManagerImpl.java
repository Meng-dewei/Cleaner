package com.cskaoyan.duolai.clean.orders.dispatch.strategys.impl;

import com.cskaoyan.duolai.clean.orders.dispatch.enums.DispatchStrategyEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.IDispatchChain;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.IDispatchChainManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DispatchChainManagerImpl implements IDispatchChainManager {
    private static final Map<DispatchStrategyEnum, IDispatchChain> DISPATCH_STRATEGY_MAP = new HashMap<>(8);

    @Override
    public void put(DispatchStrategyEnum dispatchStrategyEnum, IDispatchChain dispatchStrategy) {
        DISPATCH_STRATEGY_MAP.put(dispatchStrategyEnum, dispatchStrategy);
    }

    @Override
    public IDispatchChain get(DispatchStrategyEnum dispatchStrategyEnum) {
        return DISPATCH_STRATEGY_MAP.get(dispatchStrategyEnum);
    }
}
