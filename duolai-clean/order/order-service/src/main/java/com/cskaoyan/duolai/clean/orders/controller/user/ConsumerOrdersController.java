package com.cskaoyan.duolai.clean.orders.controller.user;

import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSimpleDTO;
import com.cskaoyan.duolai.clean.orders.request.OrdersPayCommand;
import com.cskaoyan.duolai.clean.orders.request.PlaceOrderCommand;
import com.cskaoyan.duolai.clean.orders.dto.OrdersPayDTO;
import com.cskaoyan.duolai.clean.orders.dto.PlaceOrderDTO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCreateService;
import com.cskaoyan.duolai.clean.orders.service.IOrdersManagerService;
import com.cskaoyan.duolai.clean.orders.request.OrderCancelCommand;
import com.cskaoyan.duolai.clean.orders.dto.OrderInfoDTO;
import com.cskaoyan.duolai.clean.common.model.CurrentUserInfo;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController("consumerOrdersController")
@Api(tags = "用户端-订单相关接口")
@RequestMapping("/consumer/orders")
public class ConsumerOrdersController {
    @Resource
    private IOrdersCreateService ordersCreateService;
    @Resource
    private IOrdersManagerService ordersManagerService;

    @Resource
    OrderConverter orderConverter;

    @GetMapping("/getAvailableCoupons")
    @ApiOperation("获取可用优惠券")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serveId", value = "服务id", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "purNum", value = "购买数量，默认1", required = false, dataTypeClass = Long.class)
    })
    public List<AvailableCouponsDTO> getCoupons(@RequestParam(value = "serveId", required = true) Long serveId,
                                                @RequestParam(value = "purNum", required = false, defaultValue = "1") Integer purNum) {
        return ordersCreateService.getAvailableCoupons(serveId, purNum);
    }

    @ApiOperation("下单接口")
    @PostMapping("/place")
    public PlaceOrderDTO place(@RequestBody PlaceOrderCommand placeOrderCommand) {
        return ordersCreateService.placeOrder(placeOrderCommand);
    }

    @GetMapping("/consumerQueryList")
    @ApiOperation("订单滚动分页查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ordersStatus", value = "订单状态，0：待支付，100：派单中，200：待服务，300：服务中，500：订单完成，600：订单取消，700：已关闭", required = false, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "sortBy", value = "排序字段", required = false, dataTypeClass = Long.class)
    })
    public List<OrderSimpleDTO> consumerQueryList(@RequestParam(value = "ordersStatus", required = false) Integer ordersStatus,
                                                  @RequestParam(value = "sortBy", required = false) Long sortBy) {
        return ordersManagerService.consumerQueryList(UserContext.currentUserId(), ordersStatus, sortBy);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据订单id查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class)
    })
    public OrderInfoDTO detail(@PathVariable("id") Long id) {
        return ordersManagerService.getDetail(id);
    }

    @PutMapping("/pay/{id}")
    @ApiOperation("订单支付")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class)
    })
    public OrdersPayDTO pay(@PathVariable("id") Long id, @RequestBody OrdersPayCommand ordersPayCommand) {
        return ordersCreateService.pay(id, ordersPayCommand);
    }

    @GetMapping("/pay/{id}/result")
    @ApiOperation("查询订单支付结果")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class)
    })
    public OrdersPayDTO payResult(@PathVariable("id") Long id) {
        //支付结果
        int payStatus = ordersCreateService.getPayResult(id);
        OrdersPayDTO ordersPayDTO = new OrdersPayDTO();
        ordersPayDTO.setPayStatus(payStatus);
        return ordersPayDTO;
    }

    //查询支付结果接口

    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public void cancel(@RequestBody OrderCancelCommand orderCancelCommand) {
        OrderCancelDTO orderCancelDTO = orderConverter.ordersCancelCommandToDTO(orderCancelCommand);
        CurrentUserInfo currentUserInfo = UserContext.currentUser();
        orderCancelDTO.setCurrentUserId(currentUserInfo.getId());
        orderCancelDTO.setCurrentUserName(currentUserInfo.getName());
        orderCancelDTO.setCurrentUserType(currentUserInfo.getUserType());
        orderCancelDTO.setUserId(currentUserInfo.getId());
        ordersManagerService.cancel(orderCancelDTO);
    }
}
