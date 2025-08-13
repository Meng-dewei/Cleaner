package com.cskaoyan.duolai.clean.orders.dispatch.enums;

import com.cskaoyan.duolai.clean.orders.dispatch.strategys.IDispatchChain;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.impl.DistanceDispatchChainImpl;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.impl.LeastAcceptOrderDispatchChainImpl;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DispatchStrategyEnum {
    /**
     * 距离优先策略
     */
    DISTANCE(1, new DistanceDispatchChainImpl()),
    /**
     * 最少接单策略
     */
    LEAST_ACCEPT_ORDER(2, new LeastAcceptOrderDispatchChainImpl());

    private int type;
    private IDispatchChain dispatchStrategy;

    public static DispatchStrategyEnum of(Integer type) {
        if(type == null) {
            return null;
        }
        for (DispatchStrategyEnum dispatchStrategyEnum : values()) {
            if(type.equals(dispatchStrategyEnum.type)){
                return dispatchStrategyEnum;
            }
        }
        return null;
    }


}
