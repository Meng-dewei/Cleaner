package com.cskaoyan.duolai.clean.orders.dispatch.converter;

import com.cskaoyan.duolai.clean.common.model.OrdersSeizeInfo;
import com.cskaoyan.duolai.clean.common.model.ServeProviderInfo;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderDTO;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderDispatchDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderSeizeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.ServeProviderSyncDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderSeizePageDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderSeizeConverter {


    OrderSeizePageDTO orderSeizeInfoToOrderSeizeDTO(OrdersSeizeInfo ordersSeizeInfo);

    List<OrderSeizePageDTO> orderSeizeInfosToOrderSeizeDTOs(List<OrdersSeizeInfo> ordersSeizeInfo);

    ServeProviderInfo serveProviderSyncToServeProviderInfo(ServeProviderSyncDO serveProviderSyncDO);

    List<ServeProviderInfo> serveProviderSyncsToServeProviderInfos(List<ServeProviderSyncDO> serveProviderSyncDO);

    OrdersSeizeInfo ordersSeizeToOrdersSeizeInfo(OrderSeizeDO ordersSeizeInfo);

    OrderDispatchDO ordersSeizeDOToOrdersDispatchDO(OrderSeizeDO orderSeizeDO);

    List<OrderDispatchDO> ordersSeizeDOsToOrdersDispatchDOs(List<OrderSeizeDO> orderSeizeDOS);


    OrderParam orderDTOToOrderParam(OrderDTO orderDTO);
}
