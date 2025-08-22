package com.cskaoyan.duolai.clean.orders.state;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.orders.converter.SnapshotConverter;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.enums.StatusChangeEvent;
import com.cskaoyan.duolai.clean.orders.state.core.StatusChangeHandler;
import com.cskaoyan.duolai.clean.orders.enums.StatusDefine;
import com.cskaoyan.duolai.clean.orders.state.persist.StateMachinePersister;
import com.cskaoyan.duolai.clean.orders.service.BizSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

@Slf4j
public abstract class AbstractStateMachine {

    /**
     * 状态机持久化程序
     */
    private final StateMachinePersister stateMachinePersister;

    /**
     * 业务快照服务层程序
     */
    private final BizSnapshotService bizSnapshotService;

    /**
     * redis处理程序
     */
    private RedissonClient redissonClient;

    /**
     * 初始化状态
     */
    private final StatusDefine initState;

    /**
     * 状态机名称
     */
    private final String name;

    SnapshotConverter snapshotConverter;

    /**
     * 构造方法
     *
     * @param stateMachinePersister 状态机持久化程序
     * @param bizSnapshotService    业务快照服务层程序
     * @param redissonClient         redis处理程序
     */
    protected AbstractStateMachine(StateMachinePersister stateMachinePersister, BizSnapshotService bizSnapshotService, RedissonClient redissonClient, SnapshotConverter snapshotConverter) {
        this.stateMachinePersister = stateMachinePersister;
        this.bizSnapshotService = bizSnapshotService;
        this.redissonClient = redissonClient;
        this.initState = getInitState();
        this.name = getName();
        this.snapshotConverter = snapshotConverter;
    }

    /**
     * @return 返回状态机名称
     */
    protected abstract String getName();

    /**
     * @return 初始状态
     */
    protected abstract StatusDefine getInitState();





    /**
     * 启动状态机，并设置当前状态和保存业务快照(下单的时候调用)
     *
     * @param bizId        业务id
     * @param bizSnapshot  快照
     * @return 当前状态代码
     */
    public String start(String bizId, OrderSnapshotDTO bizSnapshot) {

        //1. 查询订单是否已经有状态
        String currentState = stateMachinePersister.getCurrentState(name, bizId);
        if (ObjectUtil.isEmpty(currentState)) {
            // 还没有状态说明未初始化，执行订单状态的初始化
            stateMachinePersister.init(name, bizId, initState);
        } else {
            // 说明订单状态已存在，已经初始化过了
            throw new IllegalStateException("已存在状态，不可初始化");
        }

        //设置快照id
        bizSnapshot.setSnapshotId(bizId);
        //设置快照状态
        bizSnapshot.setSnapshotStatus(initState.getStatus());
        //快照转json
        String bizSnapshotString = JSONUtil.toJsonStr(bizSnapshot);
        if (ObjectUtil.isNotEmpty(bizSnapshot)) {
            // 保存这条订单快照
            bizSnapshotService.save(name, bizId, initState, bizSnapshotString);
        }

        return initState.getCode();
    }


    /**
     * 获取当前状态
     *
     * @param bizId 业务id
     * @return 当前状态代码
     */
    private String getCurrentState(String bizId) {
        return stateMachinePersister.getCurrentState(name, bizId);
    }

    /**
     * 获取当前快照
     *
     * @param bizId 业务id
     * @return 快照信息
     */
    public String getCurrentSnapshot(String bizId) {
        //根据当前状态和订单id查询快照中的订单数据(json字符串)
        String currentState = getCurrentState(bizId);
        return bizSnapshotService.findLastSnapshotByBizIdAndState(name, bizId, currentState);
    }

    /**
     * 根据状态查询业务快照
     *
     * @param bizId        业务id
     * @param statusDefine 状态
     * @return 业务快照
     */
    private String getSnapshotByStatus(String bizId, StatusDefine statusDefine) {
        String statusCode =  statusDefine.getCode();
        return bizSnapshotService.findLastSnapshotByBizIdAndState(name, bizId, statusCode);
    }

    /**
     * 获取当前状态的快照缓存
     *
     * @param bizId 业务id
     * @return 快照信息
     */
    public String getCurrentSnapshotCache(String bizId) {
        //先查询缓存，如果缓存没有就查询数据库然后存缓存
        String key = "STATE_MACHINE:" + name + ":" + bizId;
        RBucket<String> snapshotBucket = redissonClient.getBucket(key);
        String value = snapshotBucket.get();
        if (ObjectUtil.isNotEmpty(value)) {
            return value;
        }

        String bizSnapshot = getCurrentSnapshot(bizId);
        snapshotBucket.set(bizSnapshot, 30, TimeUnit.MINUTES);
        return bizSnapshot;
    }

    /**
     * 新增快照
     *
     * @param bizId        业务id
     * @param statusDefine 状态
     * @param bizSnapshot  业务快照
     */
    public void saveSnapshot(String bizId, StatusDefine statusDefine, OrderSnapshotDTO bizSnapshot) {
        //快照转json
        String jsonString = JSONUtil.toJsonStr(bizSnapshot);
        //新增快照
        bizSnapshotService.save(name, bizId, statusDefine, jsonString);

        //清理缓存
        String key = "STATE_MACHINE:" + name + ":" + bizId;
        redissonClient.getBucket(key).delete();
    }


    /**
     * 变更状态并保存快照，该方法的逻辑主要分为四个步骤:
     * 1. 校验状态: 判断改变订单状态时，订单的当前状态是否真的是转化前的状态
     * 2. 选择状态处理器: 根据订单的当前状态和要转化的目标状态确定要是用的状态处理器对象
     *    a. 每个状态处理器对象都通过包扫描放入到spring容器，这些对象的名字是有规律的: order_statusChangeEventEnum.getCode()
     *    b. 可以通过order_拼接order_statusChangeEventEnum.getCode()从容器中取出对应的状态处理器
     * 3. 更新修改有的状态为当前的订单状态
     * 4. 获取订单最近一次的订单快照，将其与使用方法参数bizSnapshot(最新的订单快照)的数据合并，并保存为一个新的订单快照
     *
     * @param bizId                 业务id 订单id
     * @param statusChangeEventEnum 状态变换事件
     * @param bizSnapshot           业务数据快照（json格式）
     */
    public void changeStatus(String bizId, StatusChangeEvent statusChangeEventEnum, OrderSnapshotDTO bizSnapshot) {
        // 1.校验状态
        //查询当前状态
        String statusCode = getCurrentState(bizId);

        //校验起止状态是否与事件匹配
        if (ObjectUtil.isNotEmpty(statusChangeEventEnum.getSourceStatus())
                && ObjectUtil.notEqual(statusChangeEventEnum.getSourceStatus().getCode(), statusCode)) {
            throw new CommonException(HTTP_INTERNAL_ERROR, "状态机起止状态与事件不匹配");
        }

        //2.选择状态处理器，并调用状态处理器完成状态变化
        //事件代码
        String eventCode = statusChangeEventEnum.getCode();
        StatusChangeHandler bean = null;
        try {
            // name: order_状态变更事件码
            bean = SpringUtil.getBean(name + "_" + eventCode, StatusChangeHandler.class);
        } catch (Exception e) {
            log.info("不存在‘{}’StatusChangeHandler", name + "_" + eventCode);
        }
        if (bizSnapshot == null) {
            bizSnapshot = new OrderSnapshotDTO();
        }
        //设置快照id
        bizSnapshot.setSnapshotId(bizId);
        //设置目标状态
        bizSnapshot.setSnapshotStatus(statusChangeEventEnum.getTargetStatus().getStatus());
        if (ObjectUtil.isNotNull(bean)) {
            //3.执行状态变更
            bean.handler(bizId, bizSnapshot);
        }

        //4. 更新变更后的订单状态
        stateMachinePersister.persist(name, bizId, statusChangeEventEnum.getTargetStatus());

        //5、存储快照状态变更后的快照
        if (ObjectUtil.isNotEmpty(bizSnapshot)) {
            //构建新的快照信息
            bizSnapshot = buildNewSnapshot(bizId, bizSnapshot, statusChangeEventEnum.getSourceStatus());
            String newBizSnapShotString = JSONUtil.toJsonStr(bizSnapshot);
            bizSnapshotService.save(name, bizId, statusChangeEventEnum.getTargetStatus(), newBizSnapShotString);
        }

        //7.清理快照缓存
        String key = "STATE_MACHINE:" + name + ":" + bizId;
        redissonClient.getBucket(key).delete();

    }

    /**
     * 构建新的快照数据
     *
     * @param bizId        业务id
     * @param bizSnapshot  业务快照
     * @param statusDefine 状态
     * @return 业务快照（json格式）
     */
    private OrderSnapshotDTO buildNewSnapshot(String bizId, OrderSnapshotDTO bizSnapshot, StatusDefine statusDefine) {
        //1.获取上一个状态订单快照
        String currentSnapshot = getSnapshotByStatus(bizId, statusDefine);
        if (ObjectUtil.isEmpty(currentSnapshot)) {
            return bizSnapshot;
        }

        //2.将当前状态订单快照转为bean
        OrderSnapshotDTO oldOrderSnapshotDTO = JSONUtil.toBean(currentSnapshot, OrderSnapshotDTO.class);

        //3.将新的订单快照数据覆盖旧订单快照数据，忽略null
        // 合并订单快照数据，将新的订单快照数据中的非null属性值，放入到旧的订单快照对象中
        snapshotConverter.updateOrderSnapshot(bizSnapshot, oldOrderSnapshotDTO);
        return oldOrderSnapshotDTO;
    }
}
