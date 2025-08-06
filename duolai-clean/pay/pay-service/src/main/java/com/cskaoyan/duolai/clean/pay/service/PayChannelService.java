package com.cskaoyan.duolai.clean.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.pay.model.entity.PayChannel;

/**
 * @Description： 支付通道服务类
 */
public interface PayChannelService extends IService<PayChannel> {

    /**
     * 根据商户id查询渠道配置，该配置会被缓存10分钟
     *
     * @param enterpriseId 商户id
     * @param channelLabel 通道唯一标记
     * @return PayChannelEntity 交易渠道对象
     */
    PayChannel findByEnterpriseId(Long enterpriseId, String channelLabel);


}
