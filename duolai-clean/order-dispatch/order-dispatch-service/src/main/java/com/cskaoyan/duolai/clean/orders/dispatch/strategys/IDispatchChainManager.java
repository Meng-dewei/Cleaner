package com.cskaoyan.duolai.clean.orders.dispatch.strategys;


import com.cskaoyan.duolai.clean.orders.dispatch.enums.DispatchStrategyEnum;

public interface IDispatchChainManager {
    /**
     * 设置派单策略
     * @param dispatchStrategy
     */
    void put(DispatchStrategyEnum dispatchStrategyEnum, IDispatchChain dispatchStrategy);

    /**
     * 获取派单策略
     * @param dispatchStrategyEnum
     * @return
     */
    IDispatchChain get(DispatchStrategyEnum dispatchStrategyEnum);
}
