package com.cskaoyan.duolai.clean.pay.handler.wechat;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import com.cskaoyan.duolai.clean.common.constants.ErrorInfo;
import com.cskaoyan.duolai.clean.pay.annotation.PayChannel;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.pay.enums.PayChannelEnum;
import com.cskaoyan.duolai.clean.pay.enums.TradingEnum;
import com.cskaoyan.duolai.clean.pay.handler.NativePayHandler;
import com.cskaoyan.duolai.clean.pay.handler.wechat.response.WeChatResponse;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信二维码支付
 */
@Component("wechatNativePayHandler")
@PayChannel(type = PayChannelEnum.WECHAT_PAY)
public class WechatNativePayHandler implements NativePayHandler {

    @Override
    public void createDownLineTrading(Trading tradingEntity) throws CommonException {
        // 查询配置
        WechatPayHttpClient client = WechatPayHttpClient.get(tradingEntity.getEnterpriseId());
        //请求地址
        String apiPath = "/v3/pay/transactions/native";

        //请求参数
        Map<String, Object> params = MapUtil.<String, Object>builder()
                .put("mchid", client.getMchId())
                .put("appid", client.getAppId())
                .put("description", tradingEntity.getMemo())
                .put("notify_url", client.getNotifyUrl())
                .put("out_trade_no", Convert.toStr(tradingEntity.getTradingOrderNo()))
                .put("amount", MapUtil.<String, Object>builder()
                        .put("total", Convert.toInt(NumberUtil.mul(tradingEntity.getTradingAmount(), 100))) //金额，单位：分
                        .put("currency", "CNY") //人民币
                        .build())
                .build();

        try {
            WeChatResponse response = client.doPost(apiPath, params);
            if (!response.isOk()) {
                //下单失败
                throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_PAY_FAIL.getValue());
            }
            //指定统一下单code
            tradingEntity.setPlaceOrderCode(Convert.toStr(response.getStatus()));
            //二维码需要展现的信息
            tradingEntity.setPlaceOrderMsg(JSONUtil.parseObj(response.getBody()).getStr("code_url"));
            //指定统一下单json字符串
            tradingEntity.setPlaceOrderJson(JSONUtil.toJsonStr(response));

        } catch (Exception e) {
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_PAY_FAIL.getValue());
        }
    }
}
