package com.cskaoyan.duolai.clean.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.pay.constant.Constants;
import com.cskaoyan.duolai.clean.pay.enums.PayStateEnum;
import com.cskaoyan.duolai.clean.pay.mapper.TradingMapper;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;
import com.cskaoyan.duolai.clean.pay.service.BasicPayService;
import com.cskaoyan.duolai.clean.pay.service.TradingService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description：交易订单表 服务实现类
 */
@Service
public class TradingServiceImpl extends ServiceImpl<TradingMapper, Trading> implements TradingService {
    @Resource
    private BasicPayService basicPayService;
    @Override
    public Trading findTradByTradingOrderNo(Long tradingOrderNo) {
        LambdaQueryWrapper<Trading> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Trading::getTradingOrderNo, tradingOrderNo);
        Trading one = super.getOne(queryWrapper);
        return one;
    }



    @Override
    public List<Trading> findListByTradingState(PayStateEnum tradingState, Integer count) {
        count = NumberUtil.max(count, 10);
        LambdaQueryWrapper<Trading> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Trading::getTradingState, tradingState)
                .eq(Trading::getEnableFlag, Constants.YES)
                .orderByAsc(Trading::getCreateTime)
                .last("LIMIT " + count);
        return list(queryWrapper);
    }

    /**
     * 根据订单id和支付方式查询付款中的交易单
     *
     * @param productOrderNo 订单号
     * @param tradingChannel 支付渠道代码
     * @return 交易单
     */
    @Override
    public Trading queryDuringTrading(String productAppId,Long productOrderNo, String tradingChannel){
        LambdaQueryWrapper<Trading> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Trading::getProductOrderNo, productOrderNo)
                .eq(Trading::getProductAppId,productAppId)
                .eq(Trading::getTradingState, PayStateEnum.FKZ)
                .eq(Trading::getTradingChannel, tradingChannel);
        List<Trading> list = list(queryWrapper);
        Trading trading = CollectionUtil.getFirst(list);
        return trading;
    }
    /**
     * 根据订单id查询交易单
     *
     * @param productOrderNo 订单号
     * @return 交易单
     */
    @Override
    public List<Trading> queryByProductOrder(String productAppId,Long productOrderNo){
        LambdaQueryWrapper<Trading> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Trading::getProductOrderNo, productOrderNo)
                .eq(Trading::getProductAppId, productAppId);
        return list(queryWrapper);
    }
    /**
     * 根据订单id查询已付款的交易单
     *
     * @param productOrderNo 订单id
     * @return 交易单
     */
    @Override
    public Trading findFinishedTrading(String productAppId,Long productOrderNo){
        LambdaQueryWrapper<Trading> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Trading::getProductOrderNo, productOrderNo)
                .eq(Trading::getProductAppId,productAppId)
                .eq(Trading::getTradingState, PayStateEnum.YJS);
        List<Trading> list = list(queryWrapper);
        Trading first = CollectionUtil.getFirst(list);
        return first;
    }
}
