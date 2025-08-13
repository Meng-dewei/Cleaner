package com.cskaoyan.duolai.clean.orders.dispatch.controller.inner;

import com.cskaoyan.duolai.clean.order.dispatch.param.OrderSeizeParam;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.ServeStatusEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderDiversionCommonService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderSeizeService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeManagerService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.ISeizeDispatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Api(tags = "抢单内部相关接口")
@Slf4j
@RequestMapping("/inner")
public class SeizeInnerController {

    @Resource
    private IOrderSeizeService ordersSeizeService;

    @Resource
    private IOrderDiversionCommonService ordersDiversionCommonService;

    @Resource
    private ISeizeDispatchService seizeDispatchService;

    @Resource
    private IOrderServeManagerService ordersServeManagerService;


    @DeleteMapping("/dispatch/clean/pool")
    public void clearSeizeDispatchPool(@RequestParam("id") Long orderId, @RequestParam("cityCode") String cityCode) {
        seizeDispatchService.clearSeizeDispatchPool(cityCode, orderId);
    }

    @PutMapping("/dispatch/no-serve/cancel")
    public void noServeCancelByUserAndOperation(@RequestParam("id") Long orderId) {
        ordersServeManagerService.cancelByUserAndOperationWithStatus(orderId, ServeStatusEnum.NO_SERVED.getStatus());
    }

    @PutMapping("/dispatch/serving/cancel")
    public void servingCancelByUserAndOperation(@RequestParam("id") Long orderId) {
        ordersServeManagerService.cancelByUserAndOperationWithStatus(orderId, ServeStatusEnum.SERVING.getStatus());
    }
}
