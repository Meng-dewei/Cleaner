package com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.impl;

import com.cskaoyan.duolai.clean.common.utils.CollUtils;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderDispatchDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules.IDispatchRule;
import lombok.Setter;

import java.util.List;

@Setter
public abstract class AbstractIDispatchRule implements IDispatchRule {
    /**
     * 下一条规则
     */
    private IDispatchRule next;

    public AbstractIDispatchRule(IDispatchRule next) {
        this.next = next;
    }

    public abstract List<ServeProviderDispatchDTO> doFilter(List<ServeProviderDispatchDTO> originServeProviderDTOS);

    @Override
    public List<ServeProviderDispatchDTO> filter(List<ServeProviderDispatchDTO> serveProviderDTOS) {
        List<ServeProviderDispatchDTO> result = this.doFilter(serveProviderDTOS);
        if(CollUtils.size(result) > 1 && next != null) {
            return next.filter(result);
        }else {
            return result;
        }
    }

    @Override
    public IDispatchRule next() {
        return next;
    }
}
