package com.cskaoyan.duolai.clean.orders.controller.operation;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSimpleDTO;
import com.cskaoyan.duolai.clean.orders.request.OrderPageRequest;
import com.cskaoyan.duolai.clean.orders.dto.OperationOrdersDetailDTO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersManagerService;
import com.cskaoyan.duolai.clean.orders.request.OrderCancelCommand;
import com.cskaoyan.duolai.clean.common.model.CurrentUserInfo;
import com.cskaoyan.duolai.clean.common.model.PageResult;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("operationOrdersController")
@Api(tags = "运营端-订单相关接口")
@RequestMapping("/operation/orders")
public class OperationOrdersController {
    @Resource
    private IOrdersManagerService ordersManagerService;

    @Resource
    OrderConverter orderConverter;

    @GetMapping("/page")
    @ApiOperation("订单分页查询")
    public PageDTO<OrderSimpleDTO> page(OrderPageRequest orderPageRequestDTO) {
        return ordersManagerService.operationPageQuery(orderPageRequestDTO);
    }


    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public void cancel(@RequestBody OrderCancelCommand orderCancelCommand) {
        OrderCancelDTO orderCancelDTO = orderConverter.ordersCancelCommandToDTO(orderCancelCommand);
        CurrentUserInfo currentUserInfo = UserContext.currentUser();
        orderCancelDTO.setCurrentUserId(currentUserInfo.getId());
        orderCancelDTO.setCurrentUserName(currentUserInfo.getName());
        orderCancelDTO.setCurrentUserType(currentUserInfo.getUserType());
        ordersManagerService.cancel(orderCancelDTO);
    }
}
