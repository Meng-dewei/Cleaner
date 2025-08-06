package com.cskaoyan.duolai.clean.orders.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "duolai.clean.pay")
public class TradeProperties {

    /**
     * 支付宝商户id
     */
    private Long aliEnterpriseId;

    /**
     * 微信支付商户id
     */
    private Long wechatEnterpriseId;
}
