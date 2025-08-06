package com.cskaoyan.duolai.clean.pay.handler;

import com.cskaoyan.duolai.clean.pay.model.entity.Trading;

/**
 * jsapi下单处理
 */
public interface JsapiPayHandler {

    /**
     * 创建交易
     *
     * @param tradingEntity 交易单
     */
    void createJsapiTrading(Trading tradingEntity);
}
