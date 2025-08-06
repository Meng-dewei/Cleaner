package com.cskaoyan.duolai.clean.pay.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 交易单状态枚举
 */
public enum PayStateEnum {

//    DFK(1, "待付款"),
    FKZ(2, "付款中"),
    FKSB(3, "付款失败"),
    YJS(4, "已结算（已付款）"),
    QXDD(5, "取消订单"),
    MD(6, "免单"),
    GZ(7, "挂账");

    @JsonValue
    @EnumValue
    private final Integer code;
    private final String value;

    PayStateEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getValue() {
        return this.value;
    }
}
