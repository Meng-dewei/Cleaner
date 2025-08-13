package com.cskaoyan.duolai.clean.orders.dispatch.strategys;


import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderDispatchDTO;

import java.util.List;

public interface IDispatchChain {

    /**
     * 从服务人员列表中获取高优先级别的一个，如果出现多个相同优先级随机获取一个
     *
     * @param serveProviderDTOS 服务人员/机构列表
     * @return
     */
    ServeProviderDispatchDTO getDispatchServeProvider(List<ServeProviderDispatchDTO> serveProviderDTOS);
}
