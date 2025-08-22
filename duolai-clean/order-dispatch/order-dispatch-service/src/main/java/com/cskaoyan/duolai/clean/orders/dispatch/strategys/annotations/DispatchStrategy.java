package com.cskaoyan.duolai.clean.orders.dispatch.strategys.annotations;


import com.cskaoyan.duolai.clean.orders.dispatch.enums.DispatchStrategyEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DispatchStrategy {
    /**
     * 派单策略
     * @return
     */
    DispatchStrategyEnum[] value();
}

