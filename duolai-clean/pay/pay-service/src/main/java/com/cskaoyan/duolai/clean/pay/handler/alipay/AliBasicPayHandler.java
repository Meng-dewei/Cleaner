package com.cskaoyan.duolai.clean.pay.handler.alipay;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeCloseResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
import com.cskaoyan.duolai.clean.pay.annotation.PayChannel;
import com.cskaoyan.duolai.clean.pay.constant.TradingConstant;
import com.cskaoyan.duolai.clean.common.constants.ErrorInfo;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.pay.enums.PayChannelEnum;
import com.cskaoyan.duolai.clean.pay.enums.RefundStatusEnum;
import com.cskaoyan.duolai.clean.pay.enums.TradingEnum;
import com.cskaoyan.duolai.clean.pay.enums.PayStateEnum;
import com.cskaoyan.duolai.clean.pay.handler.BasicPayHandler;
import com.cskaoyan.duolai.clean.pay.model.entity.RefundRecord;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝基础支付功能的实现
 *
 * @version 1.0
 */
@Slf4j
@Component("aliBasicPayHandler")
@PayChannel(type = PayChannelEnum.ALI_PAY)
public class AliBasicPayHandler implements BasicPayHandler {

    @Override
    public Boolean queryTrading(Trading trading) throws CommonException {
        //查询配置
        Config config = AlipayConfig.getConfig(trading.getEnterpriseId());
        //Factory使用配置
        Factory.setOptions(config);
        AlipayTradeQueryResponse queryResponse;
        try {
            //调用支付宝API：通用查询支付情况
            queryResponse = Factory
                    .Payment
                    .Common()
                    .query(String.valueOf(trading.getTradingOrderNo()));
        } catch (Exception e) {
            String msg = StrUtil.format("查询支付宝统一下单失败：trading = {}", trading);
            log.error(msg, e);
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
        }

        //修改交易单状态
        trading.setTransactionId(queryResponse.getTradeNo());
        trading.setResultCode(queryResponse.getSubCode());
        trading.setResultMsg(queryResponse.getSubMsg());
        trading.setResultJson(JSONUtil.toJsonStr(queryResponse));

        boolean success = ResponseChecker.success(queryResponse);

        if (!success && "ACQ.TRADE_NOT_EXIST".equals(queryResponse.getSubCode())) {
            // 尚未扫码支付过，交易不存在
            return false;
        }
        if (success) {
            //响应成功，分析交易状态
            String tradeStatus = queryResponse.getTradeStatus();
            if (StrUtil.equals(TradingConstant.ALI_TRADE_CLOSED, tradeStatus)) {
                //支付取消：TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）
                trading.setTradingState(PayStateEnum.QXDD);
            } else if (StrUtil.equalsAny(tradeStatus, TradingConstant.ALI_TRADE_SUCCESS, TradingConstant.ALI_TRADE_FINISHED)) {
                // TRADE_SUCCESS（交易支付成功）
                // TRADE_FINISHED（交易结束，不可退款）
                trading.setTradingState(PayStateEnum.YJS);
            }

            //非最终状态不处理，当前交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）不处理
            return false;
        }

        throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
    }

    @Override
    public Boolean closeTrading(Trading trading) throws CommonException {
        //查询配置
        Config config = AlipayConfig.getConfig(trading.getEnterpriseId());
        //Factory使用配置
        Factory.setOptions(config);
        try {
            //调用支付宝API：通用查询支付情况
            AlipayTradeCloseResponse closeResponse = Factory
                    .Payment
                    .Common()
                    .close(String.valueOf(trading.getTradingOrderNo()));
            boolean success = ResponseChecker.success(closeResponse);
            if (success) {
                trading.setTradingState(PayStateEnum.QXDD);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.CLOSE_FAIL.getValue());
        }
    }

    @Override
    public Boolean refundTrading(RefundRecord refundRecord) throws CommonException {
        //查询配置
        Config config = AlipayConfig.getConfig(refundRecord.getEnterpriseId());
        //Factory使用配置
        Factory.setOptions(config);
        //调用支付宝API：通用查询支付情况
        AlipayTradeRefundResponse refundResponse;
        try {
            // 支付宝easy sdk
            refundResponse = Factory
                    .Payment
                    .Common()
                    //扩展参数：退款单号
                    .optional("out_request_no", refundRecord.getRefundNo())
                    .refund(Convert.toStr(refundRecord.getTradingOrderNo()),
                            Convert.toStr(refundRecord.getRefundAmount()));
        } catch (Exception e) {
            String msg = StrUtil.format("调用支付宝退款接口出错！refundRecord = {}", refundRecord);
            log.error(msg, e);
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, msg);
        }
        System.out.println(refundResponse.getSubCode());
        System.out.println(refundResponse.getSubMsg());
        refundRecord.setRefundId(null);
        refundRecord.setRefundCode(refundResponse.getSubCode());
        refundRecord.setRefundMsg(JSONUtil.toJsonStr(refundResponse));
        boolean success = ResponseChecker.success(refundResponse);
        if (success) {
            refundRecord.setRefundStatus(RefundStatusEnum.SUCCESS);
            return true;
        }
        throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_REFUND_FAIL.getValue());
    }

    @Override
    public Boolean queryRefundTrading(RefundRecord refundRecord) throws CommonException {
        //查询配置
        Config config = AlipayConfig.getConfig(refundRecord.getEnterpriseId());
        //Factory使用配置
        Factory.setOptions(config);
        AlipayTradeFastpayRefundQueryResponse response;
        try {
            response = Factory.Payment.Common().queryRefund(
                    Convert.toStr(refundRecord.getTradingOrderNo()),
                    Convert.toStr(refundRecord.getRefundNo()));
        } catch (Exception e) {
            log.error("调用支付宝查询退款接口出错！refundRecord = {}", refundRecord, e);
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_REFUND_FAIL.getValue());
        }

        refundRecord.setRefundCode(response.getSubCode());
        refundRecord.setRefundMsg(JSONUtil.toJsonStr(response));
        boolean success = ResponseChecker.success(response);
        if (success) {
            refundRecord.setRefundStatus(RefundStatusEnum.SUCCESS);
            return true;
        }
        throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_REFUND_FAIL.getValue());
    }

}
