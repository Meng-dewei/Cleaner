package com.cskaoyan.duolai.clean.orders.controller.inner;

import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderCanceledDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderDTO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Api(tags = "内部接口 - 订单相关接口")
@RequestMapping("/inner")
public class InnerOrderController {

    @Resource
    private IOrdersManagerService ordersManagerService;

    @Resource
    private OrderConverter orderConverter;


    @GetMapping("/{id}")
    @ApiOperation("根据订单id查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class),
    })
    public OrderDTO queryById(@PathVariable("id") Long id) {
        OrdersDO ordersDO = ordersManagerService.queryById(id);
        return orderConverter.ordersDoToOrdersDTO(ordersDO);
    }

    @GetMapping("queryByIds")
    @ApiOperation("根据订单id列表批量查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "订单id列表", required = true, dataTypeClass = Long.class),
    })
    public List<OrderDTO> queryByIds(@RequestParam("ids") List<Long> ids) {
        List<OrdersDO> ordersDOList = ordersManagerService.batchQuery(ids);
        return orderConverter.orderDOsToOrderDTOs(ordersDOList);
    }

    @ApiOperation("订单抢单成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class),
    })
    @PutMapping("seize/success/{id}")
    public void orderSeizeSuccess(@PathVariable Long id) {
        ordersManagerService.orderSeizeSuccess(id);
    }

    @ApiOperation("订单对应的服务开始服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class),
    })
    @PutMapping("serve/start/{id}")
    public void orderServeStart(@PathVariable Long id) {
        ordersManagerService.orderServeStart(id);
    }




    @ApiOperation("订单服务完成")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class),
    })
    @PutMapping("serve/finish/{id}")
    public void orderServeFinish(@PathVariable("id") Long id, @RequestBody LocalDateTime localDateTime) {
        ordersManagerService.orderServeFinish(id, localDateTime);
    }

    @ApiOperation("根据订单id查询订单取消信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单id", required = true, dataTypeClass = Long.class),
    })
    @GetMapping("canceled/{id}")
    public OrderCanceledDTO queryOrderCanceledByOrderId(@PathVariable Long id) {
        return null;
    }

}
