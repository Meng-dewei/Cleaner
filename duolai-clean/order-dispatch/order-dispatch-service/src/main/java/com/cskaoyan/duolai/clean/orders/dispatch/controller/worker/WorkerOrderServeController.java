package com.cskaoyan.duolai.clean.orders.dispatch.controller.worker;

import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeProviderCancelDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeFinishedCommand;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeStartCommand;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeDetailDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeStatusDTO;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("orders-worker")
@Api(tags = "服务端-服务单相关接口")
@RequestMapping("/dispatch/worker")
public class WorkerOrderServeController {

    @Resource
    private IOrderServeManagerService ordersServeManagerService;

    @PostMapping("/start")
    @ApiOperation("服务端开始服务")
    public void serveStart( @RequestBody ServeStartCommand serveStartCommand) {
        ordersServeManagerService.serveStart(serveStartCommand, UserContext.currentUserId());
    }

    @PostMapping("/finish")
    @ApiOperation("服务端完成服务")
    public void serveFinish( @RequestBody ServeFinishedCommand serveFinishedCommand) {
        ordersServeManagerService.serveFinished(serveFinishedCommand, UserContext.currentUserId(), UserContext.currentUser().getUserType());
    }

    @PostMapping("/cancel")
    @ApiOperation("服务端取消服务")
    public void cancel(@RequestBody @Validated OrderServeProviderCancelDTO orderServeProviderCancelDTO) {
        ordersServeManagerService.cancelByProvider(orderServeProviderCancelDTO, UserContext.currentUserId());
    }
    @GetMapping("/{id}")
    @ApiOperation("获取服务单详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "服务单id", required = true, dataTypeClass = Long.class),
    })
    public OrderServeDetailDTO getDetail(@PathVariable("id") Long id) {
        return ordersServeManagerService.getDetail(id, UserContext.currentUserId());
    }

    @GetMapping("/status/num")
    @ApiOperation("服务端服务单状态数量,只统计待服务、待完成状态的订单")
    public OrderServeStatusDTO countServeStatusNum() {
        return ordersServeManagerService.countServeStatusNum(UserContext.currentUserId());
    }
}
