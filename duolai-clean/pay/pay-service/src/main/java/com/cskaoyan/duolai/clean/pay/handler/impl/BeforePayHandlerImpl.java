package com.cskaoyan.duolai.clean.pay.handler.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.cskaoyan.duolai.clean.common.constants.ErrorInfo;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.pay.handler.BeforePayHandler;
import com.cskaoyan.duolai.clean.pay.model.entity.RefundRecord;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;
import com.cskaoyan.duolai.clean.pay.service.RefundRecordService;
import com.cskaoyan.duolai.clean.pay.service.TradingService;
import com.cskaoyan.duolai.clean.pay.enums.RefundStatusEnum;
import com.cskaoyan.duolai.clean.pay.enums.TradingEnum;
import com.cskaoyan.duolai.clean.pay.enums.PayStateEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 交易前置处理接口
 */
@Component
public class BeforePayHandlerImpl implements BeforePayHandler {

    @Resource
    private TradingService tradingService;

    @Resource
    private RefundRecordService refundRecordService;


    @Override
    public void checkCreateTrading(Trading tradingEntity) {
        //校验不为为空，订单备注、订单号、企业号、交易金额、支付渠道
        boolean flag = ObjectUtil.isAllNotEmpty(tradingEntity,
//                tradingEntity.getMemo(),
                tradingEntity.getProductOrderNo(),
                tradingEntity.getEnterpriseId(),
                tradingEntity.getTradingAmount(),
                tradingEntity.getTradingChannel());
        //金额不能小于等于0
        boolean flag2 = !NumberUtil.isLessOrEqual(tradingEntity.getTradingAmount(), BigDecimal.valueOf(0));
        if (!flag || !flag2) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.CONFIG_ERROR.getValue());
        }

        List<Trading> tradings = tradingService.queryByProductOrder(tradingEntity.getProductAppId(),tradingEntity.getProductOrderNo());
        if (ObjectUtil.isEmpty(tradings)) {
            //新交易单，生成交易号
            tradingEntity.setTradingOrderNo(IdUtil.getSnowflakeNextId());
            return ;
        }
        //找到已付款的记录
        Trading finishedTrading = tradingService.findFinishedTrading(tradingEntity.getProductAppId(),tradingEntity.getProductOrderNo());
        if (ObjectUtil.isNotEmpty(finishedTrading)) {
            //存在已付款单子直接抛出重复支付异常
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.TRADING_STATE_SUCCEED.getValue());
        }
        //找到该支付渠道支付中的单子
        Trading trading = tradingService.queryDuringTrading(tradingEntity.getProductAppId(),tradingEntity.getProductOrderNo(), tradingEntity.getTradingChannel());
        if (ObjectUtil.isNotEmpty(trading)) {
            //存在相同支付渠道的付款中单子
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.TRADING_STATE_PAYING.getValue());
        }
        //新交易单，生成交易号
        tradingEntity.setTradingOrderNo(IdUtil.getSnowflakeNextId());
    }

    @Override
    public void checkQueryTrading(Trading trading) {
        if (ObjectUtil.isEmpty(trading)) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NOT_FOUND.getValue());
        }

    }


    @Override
    public void checkRefundTrading(Trading trading,BigDecimal refundAmount) {
        if (ObjectUtil.isEmpty(trading)) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NOT_FOUND.getValue());
        }

        if (trading.getTradingState() != PayStateEnum.YJS) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_REFUND_FAIL.getValue());
        }

        //退款总金额不可超实付总金额
        if (NumberUtil.isGreater(refundAmount, trading.getTradingAmount())) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.BASIC_REFUND_OUT_FAIL.getValue());
        }

    }

    @Override
    public void checkQueryRefundTrading(RefundRecord refundRecord) {
        if (ObjectUtil.isEmpty(refundRecord)) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.REFUND_NOT_FOUND.getValue());
        }

        if (ObjectUtil.equals(refundRecord.getRefundStatus(), RefundStatusEnum.SUCCESS)) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.REFUND_ALREADY_COMPLETED.getValue());
        }
    }

    @Override
    public void checkCloseTrading(Trading trading) {
        if (ObjectUtil.notEqual(PayStateEnum.FKZ, trading.getTradingState())) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.CLOSE_FAIL.getValue());
        }
    }
}
