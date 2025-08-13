package com.cskaoyan.duolai.clean.orders.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.DbRuntimeException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeDetailDTO;
import com.cskaoyan.duolai.clean.common.utils.DateUtils;
import com.cskaoyan.duolai.clean.common.utils.IdUtils;
import com.cskaoyan.duolai.clean.common.utils.NumberUtils;
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.market.dto.CouponUseDTO;
import com.cskaoyan.duolai.clean.market.request.CouponUseParam;
import com.cskaoyan.duolai.clean.orders.client.*;
import com.cskaoyan.duolai.clean.orders.constants.OrderConstants;
import com.cskaoyan.duolai.clean.orders.constants.RedisConstants;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderDTO;
import com.cskaoyan.duolai.clean.orders.request.OrdersPayCommand;
import com.cskaoyan.duolai.clean.orders.request.PlaceOrderCommand;
import com.cskaoyan.duolai.clean.orders.dto.OrdersPayDTO;
import com.cskaoyan.duolai.clean.orders.dto.PlaceOrderDTO;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import com.cskaoyan.duolai.clean.orders.properties.TradeProperties;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCreateService;
import com.cskaoyan.duolai.clean.common.expcetions.BadRequestException;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.pay.param.NativePayParam;
import com.cskaoyan.duolai.clean.pay.dto.NativePayDTO;
import com.cskaoyan.duolai.clean.pay.dto.TradingResDTO;
import com.cskaoyan.duolai.clean.pay.enums.PayChannelEnum;
import com.cskaoyan.duolai.clean.pay.enums.PayStateEnum;
import com.cskaoyan.duolai.clean.pay.msg.PayStatusMsg;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
import com.cskaoyan.duolai.clean.orders.enums.OrderPayStatusEnum;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusChangeEventEnum;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.model.mapper.OrdersMapper;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.user.dto.AddressBookDTO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.cskaoyan.duolai.clean.common.constants.ErrorInfo.Code.TRADE_FAILED;

/**
 * <p>
 * 下单服务类
 * </p>
 */
@Slf4j
@Service
public class OrdersCreateServiceImpl extends ServiceImpl<OrdersMapper, OrdersDO> implements IOrdersCreateService {

    @Resource
    private ServeApi serveApi;

    @Resource
    private TradingApi tradingApi;

    @Resource
    private AddressBookApi addressBookApi;

    @Resource
    private IOrdersCreateService owner; //代理对象

    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private TradeProperties tradeProperties;
    @Resource
    private NativePayApi nativePayApi;

    @Resource
    private CouponApi couponApi;

    @Resource
    private OrderConverter orderConverter;


    @Autowired
    OrdersMapper ordersMapper;

    @Resource
    OrderDispatchApi ordersSeizeApi;

    @Value("${duolai.clean.openPay}")
    Boolean openPay;


    @Override
    public List<AvailableCouponsDTO> getAvailableCoupons(Long serveId, Integer purNum) {
        // 1.获取服务
        ServeDetailDTO serveResDTO = serveApi.findById(serveId);
        if (serveResDTO == null || serveResDTO.getSaleStatus() != 2) {
            throw new BadRequestException("服务不可用");
        }
        // 2.计算订单总金额
        BigDecimal totalAmount = serveResDTO.getPrice().multiply(new BigDecimal(purNum));
        // 3.获取可用优惠券,并返回优惠券列表
        return couponApi.getAvailable(totalAmount);
    }

    @Override
    public PlaceOrderDTO placeOrder(PlaceOrderCommand placeOrderCommand) {
        // 1.数据校验
        // 校验服务地址(调用用户服务)
        AddressBookDTO address = addressBookApi.detail(placeOrderCommand.getAddressBookId());
        if (address == null) {
            throw new BadRequestException("预约地址异常，无法下单");
        }
        // 调用家政服务
        ServeDetailDTO serveDetailDTO = serveApi.findById(placeOrderCommand.getServeId());
        //服务下架不可下单
        if (serveDetailDTO == null || serveDetailDTO.getSaleStatus() != 2) {
            throw new BadRequestException("服务不可用");
        }


        // 2.下单前数据准备
        OrdersDO orderDO = orderConverter.commandAddressBookAndServeDetailToOrderDO(placeOrderCommand, address, serveDetailDTO);
        // 设置主键值(根据雪花算法生成的主键值)
        orderDO.setId(IdUtils.getSnowflakeNextId());
        orderDO.setUserId(UserContext.currentUserId());
        // 订单状态
        orderDO.setOrdersStatus(OrderStatusEnum.NO_PAY.getStatus());
        // 支付状态，暂不支持，初始化一个空状态
        orderDO.setPayStatus(OrderPayStatusEnum.NO_PAY.getStatus());

        String serveAddress = new StringBuffer(address.getProvince())
                .append(address.getCity())
                .append(address.getCounty())
                .append(address.getAddress())
                .toString();
        orderDO.setServeAddress(serveAddress);
        // 计算
        // 订单总金额 价格 * 购买数量
        orderDO.setTotalAmount(orderDO.getPrice().multiply(new BigDecimal(orderDO.getPurNum())));
        // 优惠金额 当前默认0
        orderDO.setDiscountAmount(BigDecimal.ZERO);
        // 实付金额 订单总金额 - 优惠金额
        orderDO.setRealPayAmount(NumberUtils.sub(orderDO.getTotalAmount(), orderDO.getDiscountAmount()));
        //排序字段
        long sortBy = orderDO.getId();
        orderDO.setSortBy(sortBy);

        // 保存订单数据
        owner.addWithCoupon(orderDO, placeOrderCommand.getCouponId());

        return new PlaceOrderDTO(orderDO.getId());
    }


    /**
     * 更新支付状态
     *
     * @param id        订单id
     * @param payStatus 支付状态
     */
    @Override
    public Boolean updatePayStatus(Long id, Integer payStatus) {
        LambdaUpdateWrapper<OrdersDO> updateWrapper = Wrappers.<OrdersDO>lambdaUpdate()
                .eq(OrdersDO::getId, id)
                .ne(OrdersDO::getPayStatus, payStatus)
                .set(OrdersDO::getPayStatus, payStatus);
        return super.update(updateWrapper);
    }

    /**
     * 更新退款状态
     *
     * @param id           订单id
     * @param refundStatus 退款状态
     * @param refundId     第三方支付的退款单号
     * @param refundNo     支付服务退款单号
     */
    @Override
    public Boolean updateRefundStatus(Long id, Integer refundStatus, String refundId, Long refundNo) {
        LambdaUpdateWrapper<OrdersDO> updateWrapper = Wrappers.<OrdersDO>lambdaUpdate()
                .eq(OrdersDO::getId, id)
                .ne(OrdersDO::getRefundStatus, refundStatus)
                .set(OrdersDO::getRefundStatus, refundStatus)
                .set(ObjectUtil.isNotEmpty(refundId), OrdersDO::getRefundId, refundId)
                .set(ObjectUtil.isNotEmpty(refundNo), OrdersDO::getRefundNo, refundNo);
        return super.update(updateWrapper);
    }

    /**
     * 查询超时订单id列表
     *
     * @param count 数量
     * @return 订单id列表
     */
    @Override
    public List<OrdersDO> queryOverTimePayOrdersListByCount(Integer count) {
        LambdaQueryWrapper<OrdersDO> queryWrapper = Wrappers.<OrdersDO>lambdaQuery()
                .eq(OrdersDO::getOrdersStatus, OrderStatusEnum.NO_PAY.getStatus())
                // 已经超过15分钟的订单超时时间了
                .lt(OrdersDO::getCreateTime, LocalDateTime.now().minusMinutes(15))
                .gt(OrdersDO::getId, 0)
                .gt(OrdersDO::getUserId, 0)
                .orderByAsc(OrdersDO::getCreateTime)
                .last("LIMIT " + count);

        List<OrdersDO> ordersDOList = baseMapper.selectList(queryWrapper);
        if (ObjectUtil.isEmpty(ordersDOList)) {
            return Collections.emptyList();
        }

        return ordersDOList;
    }

    /**
     * 支付成功， 其他信息暂且不填
     *
     * @param payStatusMsg 交易状态消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(PayStatusMsg payStatusMsg) {
        //第三方支付单号校验
        if (ObjectUtil.isEmpty(payStatusMsg.getTransactionId())) {
            throw new CommonException("支付成功通知缺少第三方支付单号");
        }

        /*
            修改订单状态和支付状态: id: payStatusMsg.getProductOrderNo() 订单id
            1. payTime: LocalDateTime.now()
            2. transactionId: payStatusMsg.getTransactionId()
            3. payStatus: OrderPayStatusEnum.PAY_SUCCESS.getStatus()
            4. orderStatus: OrderStatusEnum.DISPATCHING.getStatus()
            5. 更新
         */
        OrdersDO ordersDO = new OrdersDO();
        ordersDO.setId(payStatusMsg.getProductOrderNo());
        ordersDO.setPayTime(LocalDateTime.now());
        ordersDO.setTradingOrderNo(payStatusMsg.getTradingOrderNo());
        ordersDO.setTradingChannel(payStatusMsg.getTradingChannel());
        ordersDO.setTransactionId(payStatusMsg.getTransactionId());
        ordersDO.setPayStatus(OrderPayStatusEnum.PAY_SUCCESS.getStatus());
        ordersDO.setOrdersStatus(OrderStatusEnum.DISPATCHING.getStatus());
        ordersMapper.updateById(ordersDO);

        // 订单分流
        OrdersDO ordersDO1 = baseMapper.selectById(payStatusMsg.getProductOrderNo());
        OrderDTO ordersDivisionDTO = orderConverter.ordersDoToOrdersDTO(ordersDO1);
        OrderParam orderParam = orderConverter.orderDTOToOrderParam(ordersDivisionDTO);

        ordersSeizeApi.orderDivision(orderParam);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(OrdersDO ordersDO) {
        boolean save = this.save(ordersDO);
        if (!save) {
            throw new DbRuntimeException("下单失败");
        }
    }

    @Override
    public int getPayResult(Long id) {
        //查询订单表
        OrdersDO ordersDO = baseMapper.selectById(id);
        if (ObjectUtil.isNull(ordersDO)) {
            throw new CommonException(TRADE_FAILED, "订单不存在");
        }
        //支付结果
        Integer payStatus = ordersDO.getPayStatus();
        //未支付且已存在支付服务的交易单号此时远程调用支付服务查询支付结果
        if (ObjectUtil.equal(OrderPayStatusEnum.NO_PAY.getStatus(), payStatus)
                && ObjectUtil.isNotEmpty(ordersDO.getTradingOrderNo())) {
            //远程调用支付服务查询支付结果: tradingApi.findTradResultByTradingOrderNo(ordersDO.getTradingOrderNo());
            TradingResDTO tradingResDTO = tradingApi.findTradResultByTradingOrderNo(ordersDO.getTradingOrderNo());
            //如果支付成功这里更新订单状态
            if (ObjectUtil.isNotNull(tradingResDTO)
                    && ObjectUtil.equals(tradingResDTO.getTradingState(), PayStateEnum.YJS)) {
                //设置订单的支付状态成功
                PayStatusMsg msg = PayStatusMsg.builder()
                        .productOrderNo(ordersDO.getId())
                        .tradingChannel(tradingResDTO.getTradingChannel())
                        .statusCode(PayStateEnum.YJS.getCode())
                        .tradingOrderNo(tradingResDTO.getTradingOrderNo())
                        .transactionId(tradingResDTO.getTransactionId())
                        .build();

                // 调用paySuccess方法, 更新订单状态
                paySuccess(msg);
                return OrderPayStatusEnum.PAY_SUCCESS.getStatus();
            }
        }
        return payStatus;
    }

    /**
     * 订单支付
     *
     * @param id               订单id
     * @param ordersPayCommand 订单支付请求体
     * @return 订单支付响应体
     */
    @Override
    public OrdersPayDTO pay(Long id, OrdersPayCommand ordersPayCommand) {
        OrdersDO ordersDO = baseMapper.selectById(id);
        if (ObjectUtil.isNull(ordersDO)) {
            throw new CommonException(TRADE_FAILED, "订单不存在");
        }

        // 订单的支付状态为成功直接返回
        if (OrderPayStatusEnum.PAY_SUCCESS.getStatus() == ordersDO.getPayStatus()
                && ObjectUtil.isNotEmpty(ordersDO.getTradingOrderNo())) {
            return orderConverter.ordersDOToOrdersPayDTO(ordersDO);
        }

        //生成二维码
        NativePayDTO nativePayDTO = generateQrCode(ordersDO, ordersPayCommand.getTradingChannel());
        OrdersPayDTO ordersPayDTO = orderConverter.nativePayDTOToOrdersPayDTO(nativePayDTO);
        return ordersPayDTO;
    }


    //生成二维码
    private NativePayDTO generateQrCode(OrdersDO ordersDO, PayChannelEnum tradingChannel) {
        //判断支付渠道
        Long enterpriseId = ObjectUtil.equal(PayChannelEnum.ALI_PAY, tradingChannel) ?
                tradeProperties.getAliEnterpriseId() : tradeProperties.getWechatEnterpriseId();

        //构建支付请求参数
        NativePayParam nativePayCommand = new NativePayParam();
        // 订单id
        nativePayCommand.setProductOrderNo(ordersDO.getId());
        // 支付渠道
        nativePayCommand.setTradingChannel(tradingChannel);
        // 实际支付金额
        nativePayCommand.setTradingAmount(ordersDO.getRealPayAmount());
        // 商户id
        nativePayCommand.setEnterpriseId(enterpriseId);
        // 业务系统标识
        nativePayCommand.setProductAppId(OrderConstants.PRODUCT_APP_ID);//指定支付来源是家政订单
        nativePayCommand.setMemo("家政服务");//指定支付来源是家政订单

        //判断是否切换支付渠道
        if (ObjectUtil.isNotEmpty(ordersDO.getTradingChannel())
                && ObjectUtil.notEqual(ordersDO.getTradingChannel(), tradingChannel.toString())) {
            nativePayCommand.setChangeChannel(true);
        }

        //调用支付服务，生成支付二维码：nativePayApi.createDownLineTrading(nativePayCommand)
        NativePayDTO downLineTrading = nativePayApi.createDownLineTrading(nativePayCommand);

        /*
             将交易信息更新到订单中：订单id为productOrderNo
             1. 更新订单对应的交易单号tradingNo(支付服务为我们的支付交易交易单号): downLineTrading.getTradingOrderNo()
             2. 更新订单对应的交易单号tradingChannel: downLineTrading.getTradingChannel()
         */
        LambdaUpdateWrapper<OrdersDO> updateWrapper = Wrappers.<OrdersDO>lambdaUpdate()
                .eq(OrdersDO::getId, downLineTrading.getProductOrderNo())
                .set(OrdersDO::getTradingOrderNo, downLineTrading.getTradingOrderNo())
                .set(OrdersDO::getTradingChannel, downLineTrading.getTradingChannel());
        this.update(updateWrapper);

        return downLineTrading;
    }


    // 分布式事务(全局事务)
//    @Transactional
    @Override
    @GlobalTransactional
    public void addWithCoupon(OrdersDO ordersDO, Long couponId) {
        //保存订单
        owner.add(ordersDO);

        if (ObjectUtils.isNotNull(couponId)) {
            // 使用了优惠卷
            CouponUseParam couponUseCommand = new CouponUseParam();
            couponUseCommand.setOrdersId(ordersDO.getId());
            couponUseCommand.setId(couponId);
            couponUseCommand.setTotalAmount(ordersDO.getTotalAmount());
            //优惠券核销
            CouponUseDTO couponUseDTO = couponApi.use(couponUseCommand);
            // 设置优惠金额
            ordersDO.setDiscountAmount(couponUseDTO.getDiscountAmount());
            // 计算实付金额
            BigDecimal realPayAmount = ordersDO.getTotalAmount().subtract(couponUseDTO.getDiscountAmount());
            ordersDO.setRealPayAmount(realPayAmount);
            ordersDO.setDiscountAmount(couponUseDTO.getDiscountAmount());

            // 更新优惠金额和实际支付金额
            updateById(ordersDO);
        }
    }


}
