package com.cskaoyan.duolai.clean.pay.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.cskaoyan.duolai.clean.pay.constant.Constants;
import com.cskaoyan.duolai.clean.pay.constant.TradingCacheConstant;
import com.cskaoyan.duolai.clean.common.constants.ErrorInfo;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.pay.enums.RefundStatusEnum;
import com.cskaoyan.duolai.clean.pay.enums.TradingEnum;
import com.cskaoyan.duolai.clean.pay.enums.PayStateEnum;
import com.cskaoyan.duolai.clean.pay.handler.BasicPayHandler;
import com.cskaoyan.duolai.clean.pay.handler.BeforePayHandler;
import com.cskaoyan.duolai.clean.pay.handler.HandlerFactory;
import com.cskaoyan.duolai.clean.pay.model.entity.RefundRecord;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;
import com.cskaoyan.duolai.clean.pay.dto.RefundRecordDTO;
import com.cskaoyan.duolai.clean.pay.dto.TradingDTO;
import com.cskaoyan.duolai.clean.pay.service.BasicPayService;
import com.cskaoyan.duolai.clean.pay.service.RefundRecordService;
import com.cskaoyan.duolai.clean.pay.service.TradingService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 支付的基础功能
 */
@Slf4j
@Service
public class BasicPayServiceImpl implements BasicPayService {

    @Resource
    private BeforePayHandler beforePayHandler;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private TradingService tradingService;
    @Resource
    private RefundRecordService refundRecordService;

    @Override
    public TradingDTO queryTradingResult(Long tradingOrderNo) throws CommonException {
        //通过单号查询交易单数据
        Trading trading = this.tradingService.findTradByTradingOrderNo(tradingOrderNo);
        if(ObjectUtil.isNull(trading)){
            return null;
        }
        //如果已付款或已取消直接返回
        if(StrUtil.equalsAny(trading.getTradingState().getValue(), PayStateEnum.YJS.getValue(), PayStateEnum.QXDD.getValue())){
            return BeanUtil.toBean(trading,TradingDTO.class);
        }
        //查询前置处理：检测交易单参数
        this.beforePayHandler.checkQueryTrading(trading);
        //支付状态
        PayStateEnum tradingState = trading.getTradingState();
        //如果支付成功或支付取消就直接返回
        if (ObjectUtil.equal(tradingState, PayStateEnum.YJS) || ObjectUtil.equal(tradingState, PayStateEnum.QXDD)) {
            return BeanUtil.toBean(trading, TradingDTO.class);
        }
        String key = TradingCacheConstant.QUERY_PAY + tradingOrderNo;
        RLock lock = redissonClient.getFairLock(key);
        try {
            //获取锁
            if (lock.tryLock(TradingCacheConstant.REDIS_WAIT_TIME, TimeUnit.SECONDS)) {
                //选取不同的支付渠道实现
                BasicPayHandler handler = HandlerFactory.get(trading.getTradingChannel(), BasicPayHandler.class);
                Boolean result = handler.queryTrading(trading);
                if (result) {
                    //如果交易单已经完成，需要将二维码数据删除，节省数据库空间，如果有需要可以再次生成
                    if (ObjectUtil.equal(trading.getTradingState(), PayStateEnum.YJS) || ObjectUtil.equal(trading.getTradingState(), PayStateEnum.QXDD)) {
                        trading.setQrCode("");
                    }
                    //更新数据
                    this.tradingService.saveOrUpdate(trading);
                }
                return BeanUtil.toBean(trading, TradingDTO.class);
            }
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询交易单数据异常: trading = {}", trading, e);
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
        } finally {
            lock.unlock();
        }
    }

//    @Override
//    @Transactional
//    public ExecutionResultResDTO refundTradingByTradingOrderNo(Long tradingOrderNo, BigDecimal refundAmount) throws CommonException {
//        //根据业务订单号查看交易单信息
//        Trading trading = this.tradingService.findTradByTradingOrderNo(tradingOrderNo);
//        if(ObjectUtil.isEmpty(trading)){
//            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NOT_FOUND.getValue());
//        }
//        //只有已付款的交易单方可退款
//        if(ObjectUtil.notEqual(TradingStateEnum.YJS,trading.getTradingState())){
//            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.REFUND_FAIL.getValue());
//        }
//
//        ExecutionResultResDTO executionResultResDTO = refundTrading(trading.getTradingOrderNo(), refundAmount);
//        return  executionResultResDTO;
//    }

    @Override
    @Transactional
    public RefundRecord refundTrading(Long tradingOrderNo, BigDecimal refundAmount) throws CommonException {
        //通过单号查询交易单数据
        Trading trading = this.tradingService.findTradByTradingOrderNo(tradingOrderNo);
        //入库前置检查
        this.beforePayHandler.checkRefundTrading(trading,refundAmount);
        String key = TradingCacheConstant.REFUND_PAY + trading.getTradingOrderNo();
        RLock lock = redissonClient.getFairLock(key);
        try {
            //获取锁
            if (lock.tryLock(TradingCacheConstant.REDIS_WAIT_TIME, TimeUnit.SECONDS)) {

                //对于退款中的记录需要同步退款状态
                syncRefundResult(tradingOrderNo);
                //查询退款记录
                List<RefundRecord> refundRecordList = this.refundRecordService.findByTradingOrderNo(trading.getTradingOrderNo());
                //取出退款成功或退款中的记录
                List<RefundRecord> collect = refundRecordList.stream().filter(r -> StrUtil.equalsAny(r.getRefundStatus().getValue(),RefundStatusEnum.SENDING.getValue(),RefundStatusEnum.SUCCESS.getValue())).collect(Collectors.toList());
                //当没有退款成功和退款中的记录时方可继续退款
                if(ObjectUtil.isEmpty(collect)){
                    //设置退款金额
                    trading.setRefund(refundAmount);

                    RefundRecord refundRecord = new RefundRecord();
                    //退款单号
                    refundRecord.setRefundNo(IdUtil.getSnowflakeNextId());
                    refundRecord.setTradingOrderNo(trading.getTradingOrderNo());
                    refundRecord.setProductOrderNo(trading.getProductOrderNo());
                    refundRecord.setProductAppId(trading.getProductAppId());
                    refundRecord.setRefundAmount(refundAmount);
                    refundRecord.setEnterpriseId(trading.getEnterpriseId());
                    refundRecord.setTradingChannel(trading.getTradingChannel());
                    refundRecord.setTotal(trading.getTradingAmount());
                    //初始状态为退款中
                    refundRecord.setRefundStatus(RefundStatusEnum.APPLY_REFUND);
                    this.refundRecordService.save(refundRecord);
                    //设置交易单是退款订单
                    trading.setIsRefund(Constants.YES);
                    this.tradingService.saveOrUpdate(trading);

                    //请求第三方退款
                    //选取不同的支付渠道实现
                    BasicPayHandler handler = HandlerFactory.get(refundRecord.getTradingChannel(), BasicPayHandler.class);
                    Boolean result = handler.refundTrading(refundRecord);
                    if (result) {
                        //更新退款记录数据
                        this.refundRecordService.saveOrUpdate(refundRecord);
                    }
                    return refundRecord;
                }
                //取出第一条记录返回
                RefundRecord first = CollectionUtil.getFirst(refundRecordList);
                if(ObjectUtil.isNotNull(first)){
                    return first;
                }

            }
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询交易单数据异常:{}", ExceptionUtil.stacktraceToString(e));
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
        } finally {
            lock.unlock();
        }
    }

    /***
     * 对于退款中的记录需要同步退款状态
     * @param tradingOrderNo 交易单号
     */
    @Override
    public void syncRefundResult(Long tradingOrderNo) throws CommonException{
        //查询退款记录
        List<RefundRecord> refundRecordList = this.refundRecordService.findByTradingOrderNo(tradingOrderNo);
        //存在退款中记录
        List<RefundRecord> collect = refundRecordList.stream().filter(r -> r.getRefundStatus().equals(RefundStatusEnum.SENDING)).collect(Collectors.toList());

        if (ObjectUtil.isNotEmpty(collect)) {
            collect.forEach(v->{
                queryRefundTrading(v.getRefundNo());
            });
        }

    }
    @Override
    public RefundRecordDTO queryRefundTrading(Long refundNo) throws CommonException {
        //通过单号查询交易单数据
        RefundRecord refundRecord = this.refundRecordService.findByRefundNo(refundNo);
        //查询前置处理
        this.beforePayHandler.checkQueryRefundTrading(refundRecord);

        String key = TradingCacheConstant.REFUND_QUERY_PAY + refundNo;
        RLock lock = redissonClient.getFairLock(key);
        try {
            //获取锁
            if (lock.tryLock(TradingCacheConstant.REDIS_WAIT_TIME, TimeUnit.SECONDS)) {

                //选取不同的支付渠道实现
                BasicPayHandler handler = HandlerFactory.get(refundRecord.getTradingChannel(), BasicPayHandler.class);
                Boolean result = handler.queryRefundTrading(refundRecord);
                if (result) {
                    //更新数据
                    this.refundRecordService.saveOrUpdate(refundRecord);
                }
                return BeanUtil.toBean(refundRecord, RefundRecordDTO.class);
            }
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.REFUND_FAIL.getValue());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询退款交易单数据异常: refundRecord = {}", refundRecord, e);
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.REFUND_FAIL.getValue());
        } finally {
            lock.unlock();
        }
    }

    /***
     * 关闭交易单
     * @param tradingOrderNo 交易单号
     * @return 是否成功
     */
    @Override
    public Boolean closeTrading(Long tradingOrderNo) throws CommonException {
        //通过单号查询交易单数据
        Trading trading = this.tradingService.findTradByTradingOrderNo(tradingOrderNo);
        if (ObjectUtil.isEmpty(trading)) {
            return true;
        }

        //入库前置检查
        this.beforePayHandler.checkCloseTrading(trading);

        String key = TradingCacheConstant.CLOSE_PAY + trading.getTradingOrderNo();
        RLock lock = redissonClient.getFairLock(key);
        try {
            //获取锁
            if (lock.tryLock(TradingCacheConstant.REDIS_WAIT_TIME, TimeUnit.SECONDS)) {

                //选取不同的支付渠道实现
                BasicPayHandler handler = HandlerFactory.get(trading.getTradingChannel(), BasicPayHandler.class);
                Boolean result = handler.closeTrading(trading);
                if (result) {
                    trading.setQrCode("");
                    this.tradingService.saveOrUpdate(trading);
                }
                return true;
            }
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询交易单数据异常:{}", ExceptionUtil.stacktraceToString(e));
            throw new CommonException(ErrorInfo.Code.TRADE_FAILED, TradingEnum.NATIVE_QUERY_FAIL.getValue());
        } finally {
            lock.unlock();
        }
    }
}
