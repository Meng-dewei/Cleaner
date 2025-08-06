package com.cskaoyan.duolai.clean.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.pay.constant.Constants;
import com.cskaoyan.duolai.clean.pay.mapper.PayChannelMapper;
import com.cskaoyan.duolai.clean.pay.model.entity.PayChannel;
import com.cskaoyan.duolai.clean.pay.service.PayChannelService;
import org.springframework.stereotype.Service;

@Service
public class PayChannelServiceImpl extends ServiceImpl<PayChannelMapper, PayChannel> implements PayChannelService {

    @Override
    public PayChannel findByEnterpriseId(Long enterpriseId, String channelLabel) {
        LambdaQueryWrapper<PayChannel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayChannel::getEnterpriseId, enterpriseId)
                .eq(PayChannel::getChannelLabel, channelLabel)
                .eq(PayChannel::getEnableFlag, Constants.YES);
        return super.getOne(queryWrapper);
    }

}
