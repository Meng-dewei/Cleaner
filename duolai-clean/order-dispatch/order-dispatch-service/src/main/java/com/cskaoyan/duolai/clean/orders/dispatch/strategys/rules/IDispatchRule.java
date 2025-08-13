package com.cskaoyan.duolai.clean.orders.dispatch.strategys.rules;


import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderDispatchDTO;

import java.util.List;

/**
 * 派单规则
 */
public interface IDispatchRule {

    /**
     * 根据派单规则过滤服务人员
     * @param serveProviderDTOS
     * @return
     */
    List<ServeProviderDispatchDTO> filter(List<ServeProviderDispatchDTO> serveProviderDTOS);

    /**
     * 获取下一级规则
     *
     * @return
     */
    IDispatchRule next();

}
