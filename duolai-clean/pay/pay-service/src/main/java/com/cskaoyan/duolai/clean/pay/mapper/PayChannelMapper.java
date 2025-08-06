package com.cskaoyan.duolai.clean.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cskaoyan.duolai.clean.pay.model.entity.PayChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易渠道表Mapper接口
 */
@Mapper
public interface PayChannelMapper extends BaseMapper<PayChannel> {

}
