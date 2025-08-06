package com.cskaoyan.duolai.clean.orders.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cskaoyan.duolai.clean.orders.service.BizSnapshotService;
import com.cskaoyan.duolai.clean.orders.enums.StatusDefine;
import com.cskaoyan.duolai.clean.orders.model.mapper.BizSnapshotMapper;
import com.cskaoyan.duolai.clean.orders.model.entity.BizSnapshotDO;
import org.springframework.stereotype.Component;

/**
 * 业务数据服务层
 **/
@Component
public class BizSnapshotServiceImpl implements BizSnapshotService {
    /**
     * 业务快照数据层处理程序
     */
    private final BizSnapshotMapper bizSnapshotMapper;

    /**
     * 默认分库键值，不进行分库时使用
     */
    private static final Long DEFAULT_DB_SHARD_ID = 1L;

    /**
     * 构造器
     *
     * @param bizSnapshotMapper 业务快照数据层处理程序
     */
    public BizSnapshotServiceImpl(BizSnapshotMapper bizSnapshotMapper) {
        this.bizSnapshotMapper = bizSnapshotMapper;
    }

    /**
     * 新增业务快照
     * @param stateMachineName 状态机名称
     * @param bizId            业务id
     * @param statusDefine     状态
     * @param bizSnapshot      业务快照
     */
    @Override
    public void save(String stateMachineName, String bizId, StatusDefine statusDefine, String bizSnapshot) {
        BizSnapshotDO model = BizSnapshotDO.builder()
                .id(IdUtil.getSnowflakeNextId())
                .stateMachineName(stateMachineName)
                .bizId(bizId)
                .state(statusDefine.getCode())
                .bizData(bizSnapshot).build();

        bizSnapshotMapper.insert(model);
    }

    /**
     * 根据业务id和状态查询最新业务快照
     *
     * @param stateMachineName 状态机名称
     * @param bizId            业务id
     * @param state            状态代码
     * @return 业务快照
     */
    @Override
    public String findLastSnapshotByBizIdAndState(String stateMachineName, String bizId, String state) {
        LambdaQueryWrapper<BizSnapshotDO> queryWrapper = Wrappers.<BizSnapshotDO>lambdaQuery()
                .eq(ObjectUtil.isNotEmpty(stateMachineName), BizSnapshotDO::getStateMachineName, stateMachineName)
                .eq(ObjectUtil.isNotEmpty(bizId), BizSnapshotDO::getBizId, bizId)
                .eq(ObjectUtil.isNotEmpty(state), BizSnapshotDO::getState, state)
                .orderByDesc(BizSnapshotDO::getCreateTime)
                .last("limit 1");
        BizSnapshotDO bizSnapshotDO = bizSnapshotMapper.selectOne(queryWrapper);
        return ObjectUtil.isNotNull(bizSnapshotDO)? bizSnapshotDO.getBizData():null;
    }
}
