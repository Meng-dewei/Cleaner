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


//    @Override
//    public List<AvailableCouponsDTO> getAvailableCoupons(Long serveId, Integer purNum) {
//        // 1.获取服务(调用家政服务)
//        ServeDetailDTO serveResDTO = serveApi.findById(serveId);
//        if (serveResDTO == null || serveResDTO.getSaleStatus() != 2) {
//            throw new BadRequestException("服务不可用");
//        }
//        // 2.计算订单总金额
//        BigDecimal totalAmount = serveResDTO.getPrice().multiply(new BigDecimal(purNum));
//        // 3.获取可用优惠券,并返回优惠券列表(服务调用)
//        return couponApi.getAvailable(totalAmount);
//    }

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
        owner.add(orderDO);

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
        return 0;
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
        return null;

    }


    //生成二维码
    private NativePayDTO generateQrCode(OrdersDO ordersDO, PayChannelEnum tradingChannel) {


        return null;
    }


    // 分布式事务(全局事务)
    @Transactional
    @Override
    public void addWithCoupon(OrdersDO ordersDO, Long couponId) {

    }


}
