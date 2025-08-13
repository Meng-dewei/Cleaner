package com.cskaoyan.duolai.clean.orders.dispatch.converter;

import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderCanceledDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderSeizeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderServeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeFinishedCommand;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeStartCommand;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeDetailDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeInfoDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderServeInfoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderServeConverter {

    OrderServeDTO ordersServeDOToOrdersServeDTO(OrderServeDO orderServeDO);

    List<OrderServeDTO> ordersServeDOsToOrdersServeDTOs(List<OrderServeDO> orderServeDO);


    @Mapping(target = "id", ignore = true)
    OrderServeDO orderSeizeDOToOrderServeDO(OrderSeizeDO orderSeizeDO);

    OrderServeInfoDTO orderServeDOToOrderServeInfoDTO(OrderServeDO orderServeDO);


    OrderServeDO orderStartCommandToOrderServeDO(ServeStartCommand serveStartCommand);

    OrderServeDO orderFinishedCommandToOrderServeDO(ServeFinishedCommand serveFinishedCommand);

    OrderServeDetailDTO.ServeInfo orderServeDOToOrderServeDetailServeInfo(OrderServeDO orderServeDO);


    OrderServeDetailDTO.CustomerInfo orderServeDOToOrderServeDetailCustomerInfo(OrderDTO orderDTO);

    OrderServeDetailDTO.CancelInfo orderServeDOToOrderServeDetailCancelInfo(OrderCanceledDTO orderCanceledDTO);


    ServeProviderServeInfoDTO orderServeDOToServeProviderServeInfoDTO(OrderServeDO orderServeDO);
}
