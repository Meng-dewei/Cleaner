package com.cskaoyan.duolai.clean.pay.controller.inner;

import cn.hutool.core.bean.BeanUtil;
import com.cskaoyan.duolai.clean.pay.dto.TradingResDTO;
import com.cskaoyan.duolai.clean.pay.dto.TradingDTO;
import com.cskaoyan.duolai.clean.pay.service.BasicPayService;
import com.cskaoyan.duolai.clean.pay.service.TradingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("innerTradingController")
@Api(tags = "内部接口 - 交易单服务")
@RequestMapping("/inner/tradings")
public class TradingController {

    @Resource
    private TradingService tradingService;
    @Resource
    private BasicPayService basicPayService;


    @GetMapping("/findTradResultByTradingOrderNo")
    @ApiOperation(value = "根据交易单号查询交易单的交易结果", notes = "根据交易单号查询交易单的交易结果")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tradingOrderNo", value = "交易单号", required = true, dataTypeClass = Long.class)
    })
    public TradingResDTO findTradeResultByTradingOrderNo(Long tradingOrderNo) {
        TradingDTO tradingDTO = basicPayService.queryTradingResult(tradingOrderNo);
        TradingResDTO tradingResDTO = BeanUtil.toBean(tradingDTO, TradingResDTO.class);
        return tradingResDTO;
    }



}
