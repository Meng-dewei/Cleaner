package com.cskaoyan.duolai.clean.pay.service;

import com.cskaoyan.duolai.clean.pay.model.entity.Trading;

/**
 * 二维码支付
 */
public interface NativePayService {
    /***
     * 扫码支付，收银员通过收银台或商户后台调用此接口，生成二维码后，展示给用户，由用户扫描二维码完成订单支付。
     *
     * @param changeChannel 是否切换二维码
     * @param tradingEntity 扫码支付提交参数
     * @return 交易数据
     */
    Trading createDownLineTrading(boolean changeChannel, Trading tradingEntity);
}
