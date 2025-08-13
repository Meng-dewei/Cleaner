package com.cskaoyan.duolai.clean.orders.dispatch.strategys.impl;


import com.cskaoyan.duolai.clean.common.utils.CollUtils;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderDispatchDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.IDispatchRule;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.IDispatchChain;

import java.util.List;
import java.util.Objects;

public abstract class AbstractDispatchChainImpl implements IDispatchChain {
    private final IDispatchRule dispatchRule;

    public AbstractDispatchChainImpl() {
        this.dispatchRule = getRules();
        // 校验是否配置策略
        Objects.requireNonNull(this.dispatchRule);
    }

    /**
     * 设置派单规则
     * @return
     */
    protected abstract IDispatchRule getRules();

    @Override
    public ServeProviderDispatchDTO getDispatchServeProvider(List<ServeProviderDispatchDTO> serveProviderDTOS) {
        // 1.判空
        if (CollUtils.isEmpty(serveProviderDTOS)){
            return null;
        }
        IDispatchRule dr = dispatchRule;

        // 2.根据规则选定
        serveProviderDTOS = dr.filter(serveProviderDTOS);

        // 3.数据返回
        // 3.1.只有一个合适的结果
        int size = 1;
        if((size = CollUtils.size(serveProviderDTOS)) == 1) {
            return serveProviderDTOS.get(0);
        }
        // 3.2.多个合适的结果，随机选取一个
        int randomIndex = (int) (Math.random() * size);
        return serveProviderDTOS.get(randomIndex);
    }
}
