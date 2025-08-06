package com.cskaoyan.duolai.clean.pay.controller.inner;

import cn.hutool.core.bean.BeanUtil;
import com.cskaoyan.duolai.clean.pay.param.NativePayParam;
import com.cskaoyan.duolai.clean.pay.dto.NativePayDTO;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;
import com.cskaoyan.duolai.clean.pay.service.NativePayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Native支付方式Face接口：商户生成二维码，用户扫描支付
 */
@Validated
@RestController("innerNativePayController")
@Api(tags = "内部接口 - Native支付")
@RequestMapping("/inner/native")
public class NativePayController {

    @Resource
    private NativePayService nativePayService;

    /***
     * 扫码支付，收银员通过收银台或商户后台调用此接口，生成二维码后，展示给用户，由用户扫描二维码完成订单支付。
     *
     * @param nativePayCommand 扫码支付提交参数
     * @return 扫码支付响应数据，其中包含二维码路径
     */
    @PostMapping
    @ApiOperation(value = "统一收单线下交易", notes = "统一收单线下交易")
    @ApiImplicitParam(name = "nativePayDTO", value = "扫码支付提交参数", required = true)
    public NativePayDTO createDownLineTrading(@RequestBody NativePayParam nativePayCommand) {
        Trading tradingEntity = BeanUtil.toBean(nativePayCommand, Trading.class);
        Trading trading = this.nativePayService.createDownLineTrading(nativePayCommand.isChangeChannel(),tradingEntity);
        return BeanUtil.toBean(trading, NativePayDTO.class);
    }

}
