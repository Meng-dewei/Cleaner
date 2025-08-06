package com.cskaoyan.duolai.clean.pay.enums;

/**
 * 支付渠道枚举
 */
public enum PayChannelEnum {

    ALI_PAY( "支付宝"),
    WECHAT_PAY( "微信支付");

    private final String value;

    PayChannelEnum( String value) {
        this.value = value;
    }


    public String getValue() {
        return this.value;
    }

}
