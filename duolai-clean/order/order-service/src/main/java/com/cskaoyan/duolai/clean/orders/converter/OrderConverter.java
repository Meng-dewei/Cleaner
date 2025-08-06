package com.cskaoyan.duolai.clean.orders.converter;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeDetailDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSimpleDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderDTO;
import com.cskaoyan.duolai.clean.orders.request.OrderCancelCommand;
import com.cskaoyan.duolai.clean.orders.dto.OrderInfoDTO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersCanceledDO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.dto.OperationOrdersDetailDTO;
import com.cskaoyan.duolai.clean.orders.dto.OrdersPayDTO;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import com.cskaoyan.duolai.clean.orders.request.PlaceOrderCommand;
import com.cskaoyan.duolai.clean.pay.dto.NativePayDTO;
import com.cskaoyan.duolai.clean.user.dto.AddressBookDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, builder = @Builder(disableBuilder = true))
public interface OrderConverter {

    @Mapping(source = "list", target = "list")
    @Mapping(source = "total", target = "total")
    @Mapping(source = "pages", target = "pages")
    PageDTO<OrderSimpleDTO>  toOrderSimplePage(List<OrdersDO> list, Integer total, Integer pages);

    @Mapping(source = "command.serveStartTime", target = "serveStartTime")
    @Mapping(source = "command.purNum", target = "purNum")
    @Mapping(source = "command.serveId", target = "serveId")
    @Mapping(source = "address.lon", target = "lon")
    @Mapping(source = "address.lat", target = "lat")
    @Mapping(source = "address.name", target = "contactsName")
    @Mapping(source = "address.phone", target = "contactsPhone")
    @Mapping(source = "detail.serveTypeId", target = "serveTypeId")
    @Mapping(source = "detail.serveTypeName", target = "serveTypeName")
    @Mapping(source = "detail.serveItemId", target = "serveItemId")
    @Mapping(source = "detail.serveItemName", target = "serveItemName")
    @Mapping(source = "detail.serveItemImg", target = "serveItemImg")
    @Mapping(source = "detail.unit", target = "unit")
    @Mapping(source = "detail.price", target = "price")
    @Mapping(source = "detail.cityCode", target = "cityCode")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    OrdersDO commandAddressBookAndServeDetailToOrderDO(PlaceOrderCommand command, AddressBookDTO address, ServeDetailDTO detail);

    OrderSnapshotDTO orderDOtoSnapshotDTO(OrdersDO orderDO);

    @Mapping(target = "productOrderNo", source = "id")
    OrdersPayDTO ordersDOToOrdersPayDTO(OrdersDO ordersDO);

    OrderCancelDTO ordersCancelCommandToDTO(OrderCancelCommand orderCancelCommand);

    OrdersPayDTO  nativePayDTOToOrdersPayDTO(NativePayDTO nativePayDTO);


    OrderCancelDTO ordersDOToOrderCancelDTO(OrdersDO ordersDO);

    OrderInfoDTO orderDOToOrderInfoDTO(OrdersDO ordersDO);



    OrderSimpleDTO orderDOToOrderSimpleDTO(OrdersDO ordersDO);

    List<OrderSimpleDTO> orderDOsToOrderSimpleDTOs(List<OrdersDO> ordersDOs);

    OrderCancelDTO orderDOToOrderCancelDTO(OrdersDO ordersDO);


    List<OrderDTO> orderDOsToOrderDTOs(List<OrdersDO> ordersDO);

    OrdersCanceledDO orderSnapshotDTOtoCanceledDO(OrderSnapshotDTO orderSnapshotDTO);

    @Mapping(source = "currentUserId", target = "cancellerId")
    @Mapping(source = "currentUserName", target = "cancelerName")
    @Mapping(source = "currentUserType", target = "cancellerType")
    OrdersCanceledDO orderCancelDTOtoCanceledDO(OrderCancelDTO orderCancelDTO);

    OrderCancelDTO orderSnapshotDTOtoCancelDTO(OrderSnapshotDTO orderSnapshotDTO);

    OrderInfoDTO orderSnapshotDTOtoOrderDTO(OrderSnapshotDTO orderSnapshotDTO);

    OrdersDO orderSnapshotDTOtoOrderDO(OrderSnapshotDTO orderSnapshotDTO);
    OperationOrdersDetailDTO.OrderInfo orderSnapshotDTOtoOrderInfo(OrderSnapshotDTO orderSnapshotDTO);
    OperationOrdersDetailDTO.PayInfo orderSnapshotDTOtoPayInfo(OrderSnapshotDTO orderSnapshotDTO);

    OperationOrdersDetailDTO.RefundInfo orderSnapshotDTOtoRefundInfo(OrderSnapshotDTO orderSnapshotDTO);

    OperationOrdersDetailDTO.CancelInfo orderSnapshotDTOtoCancelInfo(OrderSnapshotDTO orderSnapshotDTO);


  OrderDTO ordersDoToOrdersDTO(OrdersDO ordersDO);

  OrderParam orderDTOToOrderParam(OrderDTO orderDTO);
}
