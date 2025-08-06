package com.cskaoyan.duolai.clean.pay.handler;

import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;

public interface NativePayHandler {


    /***
     * @description 统一收单线下交易预创建
     * 收银员通过收银台或商户后台调用此接口，生成二维码后，展示给用户，由用户扫描二维码完成订单支付。
     * @param tradingEntity 交易单
     */
    void createDownLineTrading(Trading tradingEntity) throws CommonException;

}
