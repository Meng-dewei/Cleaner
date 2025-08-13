package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;

import cn.hutool.db.DbRuntimeException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.common.model.dto.PageRequest;
import com.cskaoyan.duolai.clean.common.utils.*;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeItemDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeItemSimpleDTO;
import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeProviderCancelDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.request.OrderServePageRequest;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeFinishedCommand;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeStartCommand;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderCanceledDTO;
import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeDTO;
import com.cskaoyan.duolai.clean.orders.constants.FieldConstants;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderDTO;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import com.cskaoyan.duolai.clean.orders.dispatch.client.OrderApi;
import com.cskaoyan.duolai.clean.orders.dispatch.client.ServeItemApi;
import com.cskaoyan.duolai.clean.orders.dispatch.converter.OrderSeizeConverter;
import com.cskaoyan.duolai.clean.orders.dispatch.converter.OrderServeConverter;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.BreachTypeEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.BreachRecordDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderServeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeDetailDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeInfoDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeStatusDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderServeInfoDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IBreachRecordService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderDiversionCommonService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeManagerService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IServeProviderSyncService;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.common.model.PageResult;
import com.cskaoyan.duolai.clean.mysql.utils.PageUtils;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.ServeStatusEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersServeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.cskaoyan.duolai.clean.common.constants.ErrorInfo.Code.ORDERS_CANCEL;

/**
 * <p>
 * 服务服务单 服务实现类
 * </p>
 */
@Service
@Slf4j
public class OrderServeManagerServiceImpl extends ServiceImpl<OrdersServeMapper, OrderServeDO> implements IOrderServeManagerService {

    @Resource
    private ServeItemApi serveItemApi;

    @Resource
    private IOrderDiversionCommonService ordersDiversionService;

    @Resource
    private IServeProviderSyncService serveProviderSyncService;

    @Resource
    private IBreachRecordService breachRecordService;

    @Resource
    private OrderApi orderApi;


    @Resource
    private OrderServeConverter orderServeConverter;

    @Resource
    private OrderSeizeConverter orderSeizeConverter;

    @Override
    public List<OrderServeInfoDTO> queryForList(Long currentUserId, Integer serveStatus, Long sortBy) {

        // 滚动分页查询
        LambdaQueryWrapper<OrderServeDO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(OrderServeDO::getServeProviderId, currentUserId)
                .eq(ObjectUtils.isNotNull(serveStatus), OrderServeDO::getServeStatus, serveStatus)
                .lt(ObjectUtils.isNotNull(sortBy), OrderServeDO::getSortBy, sortBy);

        Page<OrderServeDO> queryPage = new Page<>();
        queryPage.addOrder(OrderItem.desc(FieldConstants.SORT_BY));
        queryPage.setSearchCount(false);
        Page<OrderServeDO> ordersServePage = baseMapper.selectPage(queryPage, lambdaQueryWrapper);

        if (PageUtils.isEmpty(ordersServePage)) {
            return new ArrayList<>();
        }
        List<Long> orderIdList = CollUtils.getFieldValues(ordersServePage.getRecords(), OrderServeDO::getOrdersId);
        if (CollUtils.isEmpty(orderIdList)) {
            return new ArrayList<>();
        }

        // 调用订单服务获取订单数据
        List<OrderDTO> orderDTOS = orderApi.queryByIds(orderIdList);
        Map<Long, OrderDTO> ordersMap = orderDTOS.stream().collect(Collectors.toMap(OrderDTO::getId, ordersDTO -> ordersDTO));

        // 服务单列表
        List<OrderServeInfoDTO> orderServeInfoDTOS = ordersServePage.getRecords().stream().map(ordersServe -> {
            OrderServeInfoDTO orderServeInfoDTO = orderServeConverter.orderServeDOToOrderServeInfoDTO(ordersServe);
            OrderDTO orderDTO = ordersMap.get(ordersServe.getOrdersId());
            orderServeInfoDTO.setServeTypeName(orderDTO.getServeTypeName());
            orderServeInfoDTO.setServeItemName(orderDTO.getServeItemName());
            orderServeInfoDTO.setServeAddress(orderDTO.getServeAddress());
            return orderServeInfoDTO;
        }).collect(Collectors.toList());
        return orderServeInfoDTOS;
    }


    @Override
    public void deleteServe(Long id, Long serveProviderId, Integer serveProviderType) {

        // 1.校验服务单是否可以删除
        // 1.1.校验服务单是否所欲当前机构
        OrderServeDTO ordersServe = queryById(id);
        // 服务单判空
        AssertUtils.isNotNull(ordersServe, "操作失败");
        AssertUtils.equals(serveProviderId, ordersServe.getServeProviderId(), "操作失败");

        // 1.3.服务单状态，已取消订单、已退单状态的订单是可以删除的
        AssertUtils.in(ordersServe.getServeStatus(), "操作失败", ServeStatusEnum.CANCLE.getStatus());

        // 2.删除服务单
        lambdaUpdate()
                .set(OrderServeDO::getDisplay, 0)
                .eq(OrderServeDO::getId, id)
                .update();

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void serveStart(ServeStartCommand serveStartCommand, Long serveProviderId) {
        // 1.校验服务单是否可以开始
        // 1.1.校验服务单是否所欲当前机构,或服务人员
        OrderServeDO orderServeDO = queryByIdAndServeProviderId(serveStartCommand.getId(), serveProviderId);
        // 服务单判空
        AssertUtils.isNotNull(orderServeDO, "操作失败");
        AssertUtils.equals(serveProviderId, orderServeDO.getServeProviderId(), "操作失败");
        // 1.2.校验订单状态是否是待待服务
        AssertUtils.equals(orderServeDO.getServeStatus(), ServeStatusEnum.NO_SERVED.getStatus());

        // 2.订单开始服务
        OrderServeDO updateOrderServe = orderServeConverter.orderStartCommandToOrderServeDO(serveStartCommand);
        updateOrderServe.setServeStatus(ServeStatusEnum.SERVING.getStatus());
        updateOrderServe.setRealServeStartTime(DateUtils.now());

        // 修改服务单状态
        boolean updateResult = updateOrderServe(serveStartCommand.getId(), serveProviderId, updateOrderServe);
        if (!updateResult) {
            throw new DbRuntimeException("更新失败");
        }

        // 3.订单状态机推动订单修改状态
        orderApi.orderServeStart(orderServeDO.getOrdersId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void serveFinished(ServeFinishedCommand serveFinishedCommand, Long serveProviderId, Integer serveProviderType) {
        // 1.校验服务单是否可以开始
        // 1.1.校验服务单是否所欲当前机构,或服务人员
        OrderServeDTO ordersServe = queryById(serveFinishedCommand.getId());
        // 1.2.校验订单状态是否是服务中
        AssertUtils.equals(ordersServe.getServeStatus(), ServeStatusEnum.SERVING.getStatus());

        // 2.订单完成服务
        OrderServeDO updateOrderServe = orderServeConverter.orderFinishedCommandToOrderServeDO(serveFinishedCommand);
        updateOrderServe.setServeStatus(ServeStatusEnum.SERVE_FINISHED.getStatus());
        updateOrderServe.setRealServeEndTime(DateUtils.now());
        boolean updateResult = updateById(updateOrderServe);
        if (!updateResult) {
            throw new DbRuntimeException("更新失败");
        }

        // 3.服务完成订单数
        serveProviderSyncService.countServeTimesAndAcceptanceNum(serveProviderId);

        // 4.订单状态机推动订单修改状态
        orderApi.orderServeFinish(ordersServe.getOrdersId(), LocalDateTime.now());
    }


    @Override
    public OrderServeDetailDTO getDetail(Long id, Long serveProviderId) {
        // 1.校验服务单是否可以查询
        OrderServeDO ordersServe = getById(id);
        // 未查询到服务单
        AssertUtils.isNotNull(ordersServe, "查询失败");

        // 服务单信息
        ServeItemDTO serveItemResDTO = serveItemApi.findById(ordersServe.getServeItemId());
        OrderServeDetailDTO orderServeDetailDTO = new OrderServeDetailDTO();
        orderServeDetailDTO.setId(ordersServe.getId());
        orderServeDetailDTO.setServeStatus(ordersServe.getServeStatus());
        // 服务信息
        OrderServeDetailDTO.ServeInfo serveInfo = orderServeConverter.orderServeDOToOrderServeDetailServeInfo(ordersServe);
        orderServeDetailDTO.setServeInfo(serveInfo);
        serveInfo.setServeItemName(ObjectUtils.get(serveItemResDTO, ServeItemDTO::getName));
        serveInfo.setServeTypeName(ObjectUtils.get(serveItemResDTO, ServeItemDTO::getServeTypeName));
        serveInfo.setUnit(ObjectUtils.get(serveItemResDTO, ServeItemDTO::getUnit));
        // 服务数量
        serveInfo.setServeNum(ObjectUtils.get(ordersServe, OrderServeDO::getPurNum));

        // 订单信息
        orderServeDetailDTO.setOrdersInfo(new OrderServeDetailDTO.OrdersInfo(ordersServe.getId(), ordersServe.getServeStartTime(), ordersServe.getOrdersAmount()));

        OrderDTO orderInfoDTO = orderApi.queryById(ordersServe.getOrdersId());
        // 客户信息
        orderServeDetailDTO.setCustomerInfo(orderServeConverter.orderServeDOToOrderServeDetailCustomerInfo(orderInfoDTO));

        // 取消信息
        // 取消原因
        if (ServeStatusEnum.CANCLE.equals(ordersServe.getServeStatus())) {
            OrderCanceledDTO ordersCanceledDO = orderApi.queryOrderCanceledByOrderId(orderInfoDTO.getId());
            OrderServeDetailDTO.CancelInfo cancelInfo = orderServeConverter.orderServeDOToOrderServeDetailCancelInfo(ordersCanceledDO);
            orderServeDetailDTO.setCancelInfo(cancelInfo);

        }

        return orderServeDetailDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByProvider(OrderServeProviderCancelDTO orderServeProviderCancelDTO, Long serveProviderId) {

        // 1.取消服务校验
        OrderServeDTO ordersServe = queryById(orderServeProviderCancelDTO.getId());
        // 校验状态
        AssertUtils.in(ordersServe.getServeStatus(), "当前不可自行取消订单，如需取消需拨打客服热线", ServeStatusEnum.NO_SERVED.getStatus());
        if (DateUtils.between(DateUtils.now(), ordersServe.getServeStartTime()).toMinutes() < 120) {
            throw new CommonException(ORDERS_CANCEL, "当前不可自行取消订单，如需取消需拨打客服热线");
        }

        // 2.修改服务单状态为已取消
        cancelOrderServe(ordersServe.getId(), ordersServe.getServeProviderId());
        // 3.重新分流
        OrderDTO orderDTO = orderApi.queryById(ordersServe.getOrdersId());
        OrderParam orderParam = orderSeizeConverter.orderDTOToOrderParam(orderDTO);
        ordersDiversionService.diversion(orderParam);
        // 4.违约记录
        BreachRecordDO breachRecordDO = toBreachRecord(orderDTO, ordersServe.getServeProviderId(), ordersServe.getServeProviderType(), orderServeProviderCancelDTO.getCancelReason(), BreachTypeEnum.CANCEL_NO_SERVE);
        breachRecordService.add(breachRecordDO);

        // 3.服务时间重新统计
        serveProviderSyncService.countServeTimesAndAcceptanceNum(ordersServe.getServeProviderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByUserAndOperationWithStatus(Long ordersId, Integer status) {

        LambdaQueryWrapper<OrderServeDO> wrapper = Wrappers.<OrderServeDO>lambdaQuery()
                .eq(OrderServeDO::getOrdersId, ordersId)
                .eq(OrderServeDO::getServeStatus, status);

        OrderServeDO orderServeDO = baseMapper.selectOne(wrapper);

        // 2.修改服务单
        boolean update = lambdaUpdate()
                .eq(OrderServeDO::getServeStatus, status)
                .set(OrderServeDO::getServeStatus, ServeStatusEnum.CANCLE.getStatus())
                .eq(OrderServeDO::getId, orderServeDO.getId())
                .update();
        if (!update) {
            throw new DbRuntimeException("操作失败,请稍后重试");
        }
        // 3.服务时间重新统计
        serveProviderSyncService.countServeTimesAndAcceptanceNum(orderServeDO.getServeProviderId());
    }

    @Override
    public OrderServeStatusDTO countServeStatusNum(Long serveProviderId) {

        LambdaQueryWrapper<OrderServeDO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.ge(OrderServeDO::getId, 0)
                .in(OrderServeDO::getServeStatus, Arrays.asList(ServeStatusEnum.NO_ALLOCATION.getStatus(), ServeStatusEnum.NO_SERVED.getStatus(), ServeStatusEnum.SERVING.getStatus()))
                .eq(OrderServeDO::getServeProviderId, serveProviderId)
                .select(OrderServeDO::getId, OrderServeDO::getServeStatus);
        List<OrderServeDO> orderServeDOS = baseMapper.selectList(lambdaQueryWrapper);
        if (CollUtils.isEmpty(orderServeDOS)) {
            return OrderServeStatusDTO.empty();
        }
        Map<Integer, Long> statusAndNumMap = orderServeDOS.stream().collect(Collectors.groupingBy(OrderServeDO::getServeStatus, Collectors.counting()));
        // 待服务
        Long noServed = statusAndNumMap.get(ServeStatusEnum.NO_SERVED.getStatus());
        // 服务中
        Long serving = statusAndNumMap.get(ServeStatusEnum.SERVING.getStatus());
        return new OrderServeStatusDTO(NumberUtils.null2Zero(noServed), NumberUtils.null2Zero(serving));
    }

    /**
     * 查询服务人员服务数据
     *
     * @param ordersServePageQueryByCurrentUserReqDTO 分页条件
     * @return 分页结果
     */
    @Override
    public PageResult<ServeProviderServeInfoDTO> pageQueryByServeProvider(OrderServePageRequest ordersServePageQueryByCurrentUserReqDTO) {
        //1.构件查询条件
        Page<OrderServeDO> page = PageUtils.parsePageQuery(ordersServePageQueryByCurrentUserReqDTO, OrderServeDO.class);
        LambdaQueryWrapper<OrderServeDO> queryWrapper = Wrappers.<OrderServeDO>lambdaQuery()
                .eq(OrderServeDO::getServeProviderId, ordersServePageQueryByCurrentUserReqDTO.getServeProviderId())
                .eq(OrderServeDO::getServeStatus, ServeStatusEnum.SERVE_FINISHED.getStatus());
//                .lt(OrdersServe::getRealServeEndTime, LocalDateTime.now().minusDays(15))
                //TODO 测试暂时查询1小时以后的服务数据
//                .lt(OrdersServeDO::getRealServeEndTime, LocalDateTime.now().minusHours(1));

        //2.分页查询
        Page<OrderServeDO> pageResult = page(page, queryWrapper);
        if (ObjectUtils.isEmpty(pageResult.getRecords())) {
            //return PageUtils.toPage(pageResult, ServeProviderServeResDTO.class);
            return null;
        }

        List<OrderServeDO> orderServeDOList = pageResult.getRecords();

        //3.服务项数据
        List<Long> serveItemIds = orderServeDOList.stream().map(OrderServeDO::getServeItemId).collect(Collectors.toList());
        List<ServeItemSimpleDTO> serveItemSimpleResDTOList = serveItemApi.listByIds(serveItemIds);
        Map<Long, String> serveItemMap = serveItemSimpleResDTOList.stream().collect(Collectors.toMap(ServeItemSimpleDTO::getId, ServeItemSimpleDTO::getName));


        //5.组装服务项数据
        List<ServeProviderServeInfoDTO> list = orderServeDOList.stream().map(o -> {
            ServeProviderServeInfoDTO serveProviderServeInfoDTO = orderServeConverter.orderServeDOToServeProviderServeInfoDTO(o);
            serveProviderServeInfoDTO.setServeItemName(serveItemMap.get(o.getServeItemId()));
            return serveProviderServeInfoDTO;
        }).collect(Collectors.toList());


        //7.返回分页结果
        return PageResult.<ServeProviderServeInfoDTO>builder()
                .list(list)
                .pages(pageResult.getPages())
                .total(pageResult.getTotal())
                .build();
    }


    private BreachRecordDO toBreachRecord(OrderDTO ordersDO, Long serveProviderId, Integer serveProviderType, String reason, BreachTypeEnum behaviorType) {
        BreachRecordDO breachRecordDO = new BreachRecordDO();
        breachRecordDO.setId(IdUtils.getSnowflakeNextId());
        breachRecordDO.setServeProviderId(serveProviderId);
        breachRecordDO.setBehaviorType(behaviorType.getType());
        breachRecordDO.setServeItemName(ordersDO.getServeItemName());
        breachRecordDO.setServeAddress(ordersDO.getServeAddress());
        breachRecordDO.setServedUserId(ordersDO.getUserId());
        breachRecordDO.setServedPhone(ordersDO.getContactsPhone());
        breachRecordDO.setBreachReason(reason);
        breachRecordDO.setOrdersId(ordersDO.getId());
        breachRecordDO.setBreachDay(DateUtils.getDay());
        return breachRecordDO;
    }

    @Override
    public OrderServeDTO queryById(Long id) {
        List<OrderServeDO> list = lambdaQuery()
                .eq(OrderServeDO::getId, id)
                .ge(OrderServeDO::getServeProviderId, 0)
                .last(" limit 1")
                .list();
        return orderServeConverter.ordersServeDOToOrdersServeDTO(CollUtils.getFirst(list));
    }

    @Override
    public OrderServeDO queryByIdAndServeProviderId(Long id, Long serveProviderId) {
        return lambdaQuery()
                .eq(OrderServeDO::getId, id)
                .eq(OrderServeDO::getServeProviderId, serveProviderId)
                .one();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderServe(Long id, Long serveProviderId, OrderServeDO orderServeDO) {
        LambdaUpdateWrapper<OrderServeDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(OrderServeDO::getId, id)
                .eq(OrderServeDO::getServeProviderId, serveProviderId);
        return baseMapper.update(orderServeDO, lambdaUpdateWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderServe(Long id, Long serveProviderId) {
        LambdaUpdateWrapper<OrderServeDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(OrderServeDO::getId, id)
                .eq(OrderServeDO::getServeProviderId, serveProviderId)
                .set(OrderServeDO::getServeStatus, ServeStatusEnum.CANCLE.getStatus());
        int delete = baseMapper.update(null, lambdaUpdateWrapper);
        if (delete <= 0) {
            throw new DbRuntimeException("请求失败");
        }
    }

    @Override
    public PageDTO<OrderServeDTO> queryOrderServeByServeStatus(Integer status, PageRequest pageRequest) {
        LambdaQueryWrapper<OrderServeDO> queryWrapper = Wrappers.<OrderServeDO>lambdaQuery()
               .eq(OrderServeDO::getServeStatus, status)
               .last("limit 1");

        Page<OrderServeDO> pageParam
                = new Page<>(pageRequest.getPageNo(), pageRequest.getPageSize());

        Page<OrderServeDO> result = baseMapper.selectPage(pageParam, queryWrapper);
        List<OrderServeDTO> orderServeDTOS = orderServeConverter.ordersServeDOsToOrdersServeDTOs(result.getRecords());

        return PageUtils.toPage(result, orderServeDTOS);
    }

}
