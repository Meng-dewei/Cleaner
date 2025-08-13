package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.constants.ErrorInfo;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.common.utils.*;
import com.cskaoyan.duolai.clean.housekeeping.dto.ConfigRegionDTO;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeDTO;
import com.cskaoyan.duolai.clean.orders.constants.FieldConstants;
import com.cskaoyan.duolai.clean.orders.constants.OrdersOriginType;
import com.cskaoyan.duolai.clean.orders.constants.RedisConstants;
import com.cskaoyan.duolai.clean.orders.dispatch.client.OrderApi;
import com.cskaoyan.duolai.clean.orders.dispatch.client.RegionApi;
import com.cskaoyan.duolai.clean.orders.dispatch.client.ServeProviderApi;
import com.cskaoyan.duolai.clean.orders.dispatch.client.ServeSkillApi;
import com.cskaoyan.duolai.clean.orders.dispatch.converter.OrderServeConverter;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.ServeStatusEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersDispatchMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersSeizeMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.converter.OrderSeizeConverter;
import com.cskaoyan.duolai.clean.orders.dispatch.config.RedissonSeizeOrderLuaHandler;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderSeizeDO;
import com.cskaoyan.duolai.clean.common.model.OrdersSeizeInfo;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderServeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.request.OrderSerizeRequest;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderSeizePageDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderSeizeResultDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderSeizeService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IServeProviderSyncService;
import com.cskaoyan.duolai.clean.orders.utils.ServeTimeUtils;
import com.cskaoyan.duolai.clean.rocketmq.client.RocketMQClient;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import com.cskaoyan.duolai.clean.user.dto.ServeProviderInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.cskaoyan.duolai.clean.orders.constants.ErrorInfo.Msg.*;

/**
 * <p>
 * 抢单池 服务实现类
 * </p>
 */
@Service
@Slf4j
public class OrderSeizeServiceImpl extends ServiceImpl<OrdersSeizeMapper, OrderSeizeDO> implements IOrderSeizeService {

    @Resource
    private ServeProviderApi serveProviderApi;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IOrderServeService ordersServeService;

    @Resource
    private RedissonSeizeOrderLuaHandler redissonSeizeOrderLuaHandler;
    @Resource
    private ServeSkillApi serveSkillApi;

    @Resource
    OrderSeizeConverter orderSeizeConverter;


    @Resource
    private ElasticsearchRestTemplate elasticSearchTemplate;

    @Resource
    private IServeProviderSyncService serveProviderSyncService;

    @Resource
    private IOrderSeizeService ordersSeizeService;

    @Resource
    private RegionApi regionApi;

    @Resource
    private OrdersDispatchMapper ordersDispatchMapper;

    @Resource
    private RocketMQClient rocketMQClient;


    @Resource
    private OrderApi ordersApi;

    @Resource
    private OrderServeConverter orderServeConverter;

    @Resource
    private IServeProviderSyncService providerSyncService;

    @Override
    public List<OrderSeizeDO> queryNeedToDispatchSeizeOrders(String cityCode, Integer timeoutInterval) {
        //当前时间加上配置的时间间隔
        LocalDateTime maxServeStartTime = DateUtils.now().plusMinutes(timeoutInterval);
        LambdaQueryWrapper<OrderSeizeDO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(OrderSeizeDO::getCityCode, cityCode)
                // 查询当前时间距离服务预约时间间隔小于指定值
                .le(OrderSeizeDO::getServeStartTime, maxServeStartTime)
                //预约时间大于当前时间
                .ge(OrderSeizeDO::getServeStartTime, DateUtils.now());
        return baseMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchTimeout(List<Long> ids) {
        lambdaUpdate()
                .set(OrderSeizeDO::getIsTimeOut, true)
                .in(OrderSeizeDO::getId, ids)
                .update();
    }


    @Override
    public List<OrderSeizeDO> queryArriveServeStartTimeSeizeOrder() {
        return lambdaQuery()
                .le(OrderSeizeDO::getServeStartTime, DateUtils.now())
                .list();
    }

    @Override
    public List<OrderSeizePageDTO> queryForList(OrderSerizeRequest orderSerizeRequest) {

        // 1.校验是否可以查询（认证通过，开启抢单）
        ServeProviderInfoDTO detail = serveProviderApi.getDetail(UserContext.currentUserId());
        // 验证设置状态
        if (detail.getSettingsStatus() != 1 || !detail.getCanPickUp()) {
            return new ArrayList<>();
        }
        // 2.查询准备 （距离、技能）
        // 距离
        Double serveDistance = orderSerizeRequest.getServeDistance();
        if (ObjectUtils.isNull(orderSerizeRequest.getServeDistance())) {
            // 区域默认配置配置(调用家政服务)
            ConfigRegionDTO configRegionInnerResDTO = regionApi.findConfigRegionByCityCode(detail.getCityCode());
            serveDistance = configRegionInnerResDTO.getStaffServeRadius().doubleValue();
        }
        // 技能(调用家政服务)
        List<Long> serveItemIds = serveSkillApi.queryServeSkillListByServeProvider(UserContext.currentUserId(), UserContext.currentUser().getUserType(), detail.getCityCode());
        if (CollUtils.isEmpty(serveItemIds)) {
            log.info("当前服务人员没有对应技能");
            return new ArrayList<>();
        }


        // 3.查询符合条件的抢单列表id
        List<OrderSeizePageDTO> ordersSeizes = getSeizeOrders(
                serveItemIds, detail.getLon(), detail.getLat(), serveDistance, detail.getCityCode(), orderSerizeRequest);

        return ordersSeizes;
    }

    /**
     * 获取抢单id，抢单类型，抢单预约服务时间
     *
     * @param serveItemIds        服务项id
     * @param lon                 当前服务人员所在位置经度
     * @param lat                 当前服务人员所在纬度
     * @param distanceLimit       抢单距离限制
     * @param cityCode            城市编码
     * @param orderSerizeRequest 抢单查询参数
     * @return
     */
    private List<OrderSeizePageDTO> getSeizeOrders(List<Long> serveItemIds, Double lon, Double lat, double distanceLimit,
                                                   String cityCode, OrderSerizeRequest orderSerizeRequest) {


        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (orderSerizeRequest.getServeTypeId() != null) {
            // 查询服务类型
            boolQueryBuilder.must(QueryBuilders.termQuery(FieldConstants.SERVE_TYPE_ID, orderSerizeRequest.getServeTypeId()));
        }
        // 查询服务项
        boolQueryBuilder.must(QueryBuilders.termsQuery(FieldConstants.SERVE_ITEM_ID, serveItemIds));
        // 查询城市
        boolQueryBuilder.must(QueryBuilders.termQuery(FieldConstants.CITY_CODE, cityCode));
        // 查询距离范围
        boolQueryBuilder.must(QueryBuilders.geoDistanceQuery(FieldConstants.LOCATION).point(lat, lon).distance(distanceLimit, DistanceUnit.KILOMETERS));

        // 按照距离排序
        GeoDistanceSortBuilder geoDistanceSortBuilder = SortBuilders.geoDistanceSort(FieldConstants.LOCATION, lat, lon)
                .unit(DistanceUnit.KILOMETERS).geoDistance(GeoDistance.ARC).order(SortOrder.ASC);

        queryBuilder.withQuery(boolQueryBuilder);
        queryBuilder.withSort(geoDistanceSortBuilder);

        SearchHits<OrdersSeizeInfo> search = elasticSearchTemplate.search(queryBuilder.build(), OrdersSeizeInfo.class);
        return search.stream()
                .map(hit -> {
                    // 从sort字段中获取实际距离
                    double realDistance = (double) hit.getSortValues().get(0);
                    OrderSeizePageDTO ordersSeize = orderSeizeConverter.orderSeizeInfoToOrderSeizeDTO(hit.getContent());
                    ordersSeize.setRealDistance(realDistance);
                    return ordersSeize;
                }).collect(Collectors.toList());
    }

    @Override
    public void seize(Long id, Long serveProviderId, Integer serveProviderType, Boolean isMatchine) {

        // 1.抢单校验
        // 1.1.校验是否可以查询（认证通过，开启抢单）
        ServeProviderInfoDTO detail = serveProviderApi.getDetail(serveProviderId);
        if (!detail.getCanPickUp() || detail.getSettingsStatus() != 1) {
            throw new CommonException(ErrorInfo.Code.SEIZE_ORDERS_FAILD, SEIZE_ORDERS_RECEIVE_CLOSED);
        }
        // 1.2.校验抢单是否存在
        OrderSeizeDO orderSeizeDO = ordersSeizeService.getById(id);
        // 校验订单是否还存在，如果订单为空或id不存在，则认为订单已经不在
        if (orderSeizeDO == null || ObjectUtils.isNull(orderSeizeDO.getId())) {
            throw new CommonException(ErrorInfo.Code.SEIZE_ORDERS_FAILD, SEIZE_ORDERS_FAILD);
        }

        // 获取订单预约时间
        Long serveTime = ServeTimeUtils.getServeTimeLong(orderSeizeDO.getServeStartTime());


        // 调用家政服务
        ConfigRegionDTO configRegionInnerResDTO = regionApi.findConfigRegionByCityCode(detail.getCityCode());
        // 获取最大接单量阈值
        int receiveOrderMax = configRegionInnerResDTO.getStaffReceiveOrderMax();

        // 2.执行redis脚本抢单
        // 执行lua脚本抢单
        Long result = redissonSeizeOrderLuaHandler.orderSeize(id, serveProviderId,
                 detail.getCityCode(), String.valueOf(serveTime), String.valueOf(receiveOrderMax));

//        Long result = -1L;
        log.debug("抢单结果 : {}", result);

        // 3.抢单结果判断
        if (result ==  -1) {
            // 时间冲突
            throw new CommonException(ErrorInfo.Code.SEIZE_ORDERS_FAILD, SEIZE_ORDERS_SERVE_TIME_EXISTS);

        }
        if (result == -2) {
            // 接单量已达上限
            throw new CommonException(ErrorInfo.Code.SEIZE_ORDERS_FAILD, SEIZE_ORDERS_RECEIVE_ORDERS_NUM_OVER);
        }

        if (result == -3) {
            // 库存为0
            throw new CommonException(ErrorInfo.Code.SEIZE_ORDERS_FAILD, SEIZE_ORDERS_FAILD);
        }


        // 抢单成功，发送消息，异步完成抢单之后的工作
        OrderSeizeResultDTO orderSeizeResultDTO = new OrderSeizeResultDTO();
        orderSeizeResultDTO.setServeProviderId(serveProviderId);
        orderSeizeResultDTO.setOrdersId(id);
        orderSeizeResultDTO.setIsMachineSeize(isMatchine);

        // 发送消息，执行抢单后处理，
        rocketMQClient.sendMessage(MqTopicConstant.ORDERS_SEIZE_TOPIC, orderSeizeResultDTO);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seizeOrdersSuccess(Long orderSeizeId, Long serveProviderId, boolean isMachineSeize) {

        // 1.校验服务单是否已经生成
        List<OrderServeDTO> oldOrderServeList = ordersServeService.findByOrderId(orderSeizeId);
        // 判断订单是否首次生成服务单
        boolean isFirstOrderServe = CollectionUtils.isEmpty(oldOrderServeList);
        OrderSeizeDO orderSeizeDO = getById(orderSeizeId);

        // 2.生成服务单
        OrderServeDO orderServeDO = orderServeConverter.orderSeizeDOToOrderServeDO(orderSeizeDO) ;
        orderServeDO.setCreateTime(null);
        orderServeDO.setUpdateTime(null);
        // 服务单状态 待分配
        int serveStatus =  ServeStatusEnum.NO_SERVED.getStatus();
        // 服务单来源类型,人工抢单来源抢单，值为1；机器抢单来源派单，值为2
        int ordersOriginType = isMachineSeize ? OrdersOriginType.DISPATCH : OrdersOriginType.SEIZE;
        orderServeDO.setOrdersOriginType(ordersOriginType);
        orderServeDO.setOrdersId(orderSeizeDO.getId());
        orderServeDO.setServeStatus(serveStatus);
        orderServeDO.setServeProviderId(serveProviderId);

        if (!ordersServeService.save(orderServeDO)) {
            return;
        }

        // 3. 完成抢单成功的清理工作
        String resourceStockRedisKey = String.format(RedisConstants.RedisKey.ORDERS_RESOURCE_STOCK, orderSeizeDO.getCityCode());
        RMap<String, String> stockMap = redissonClient.getMap(resourceStockRedisKey, StringCodec.INSTANCE);
        Integer stock = Integer.parseInt(stockMap.get(orderSeizeDO.getId()));
        if (ObjectUtils.isNull(stock) || NumberUtils.parseInt(stock.toString()) <= 0) {
            // 删除派单记录
            ordersDispatchMapper.deleteById(orderSeizeDO.getId());

            // 删除抢单记录
            ordersSeizeService.removeById(orderSeizeDO.getId());

            // 删除抢单库存
            stockMap.remove(orderSeizeDO.getId());

        }

        //修改订单状态
        if (isFirstOrderServe) {
            // 如果是服务人员取消订单，从而导致重新派单，无需修改订单状态
            ordersApi.orderSeizeSuccess(orderSeizeDO.getId());
        }

        // 更新用户的接单数据，等到实现派单时再来添加

    }


}
