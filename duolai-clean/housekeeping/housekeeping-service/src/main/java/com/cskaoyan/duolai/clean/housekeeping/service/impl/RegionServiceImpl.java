package com.cskaoyan.duolai.clean.housekeeping.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.expcetions.ForbiddenOperationException;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.housekeeping.converter.RegionConverter;
import com.cskaoyan.duolai.clean.housekeeping.converter.RegionServeConverter;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.RegionServeDO;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionSimpleDTO;
import com.cskaoyan.duolai.clean.housekeeping.enums.HousekeepingStatusEnum;
import com.cskaoyan.duolai.clean.housekeeping.dao.mapper.CityDirectoryMapper;
import com.cskaoyan.duolai.clean.housekeeping.dao.mapper.RegionMapper;
import com.cskaoyan.duolai.clean.housekeeping.dao.mapper.RegionServeMapper;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.CityDirectoryDO;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.RegionDO;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.RegionServeDetailDO;
import com.cskaoyan.duolai.clean.housekeeping.request.RegionCommand;
import com.cskaoyan.duolai.clean.housekeeping.request.RegionPageRequest;
import com.cskaoyan.duolai.clean.housekeeping.dto.DisplayServeTypeDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionServeDetailDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeTypeHomeDTO;
import com.cskaoyan.duolai.clean.housekeeping.service.IConfigRegionService;
import com.cskaoyan.duolai.clean.housekeeping.service.IHomeService;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionServeService;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionService;
import com.cskaoyan.duolai.clean.mysql.utils.PageUtils;
import com.cskaoyan.duolai.clean.redis.constants.RedisConstants;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 区域管理
 **/
@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, RegionDO> implements IRegionService {
    @Resource
    private IConfigRegionService configRegionService;
    @Resource
    private CityDirectoryMapper cityDirectoryMapper;

    @Resource
    RegionConverter regionConverter;

    @Resource
    IRegionServeService regionServeService;

    @Resource
    IRegionService regionService;


    /**
     * 区域新增
     *
     * @param regionCommand 插入更新区域
     */
    @Override
    @Transactional
    public void addRegion(RegionCommand regionCommand) {
        //1.校验城市编码是否重复
        LambdaQueryWrapper<RegionDO> queryWrapper = Wrappers.<RegionDO>lambdaQuery().eq(RegionDO::getCityCode, regionCommand.getCityCode());
        Long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new ForbiddenOperationException("城市编码不可重复");
        }

        //查询城市
        LambdaQueryWrapper<CityDirectoryDO> cityQueryWrapper = new LambdaQueryWrapper<>();
        cityQueryWrapper.eq(CityDirectoryDO::getCityCode, regionCommand.getCityCode());
        CityDirectoryDO cityDirectoryDO = cityDirectoryMapper.selectOne(cityQueryWrapper);
        if (cityDirectoryDO == null) {
            throw new ForbiddenOperationException("城市未找到");
        }

        //查询城市的排序位
        int sortNum = cityDirectoryMapper.selectOne(cityQueryWrapper).getSortNum();

        //2.新增区域
        RegionDO regionDO = regionConverter.regionCommandToRegionDO(regionCommand);
        regionDO.setSortNum(sortNum);
        baseMapper.insert(regionDO);

        //3.初始化区域配置
        configRegionService.init(regionDO.getId(), regionDO.getCityCode());
    }

    /**
     * 区域修改
     *
     * @param id           区域id
     * @param managerName  负责人姓名
     * @param managerPhone 负责人电话
     */
    @Override
    public void update(Long id, String managerName, String managerPhone) {
        RegionDO oldReginDo = baseMapper.selectById(id);
        if(ObjectUtil.isNull(oldReginDo)){
            throw new ForbiddenOperationException("服务信息不存在");
        }
        // 草稿状态才能改
//        if (HousekeepingStatusEnum.INIT.getStatus() != oldReginDo.getActiveStatus()){
//            throw new ForbiddenOperationException("草稿状态方可修改");
//        }
        oldReginDo.setManagerName(managerName);
        oldReginDo.setManagerPhone(managerPhone);
        baseMapper.updateById(oldReginDo);
    }

    /**
     * 区域删除
     *
     * @param id 区域id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        //区域信息
        RegionDO regionDO = baseMapper.selectById(id);
        if (ObjectUtil.isNull(regionDO)) {
            throw new ForbiddenOperationException("区域信息不存在");
        }

        //草稿状态方可删除
        Integer activeSataus = regionDO.getActiveStatus();
        if (HousekeepingStatusEnum.INIT.getStatus() != activeSataus) {
            throw new ForbiddenOperationException("草稿状态方可删除");
        }

        //删除
        baseMapper.deleteById(id);
    }

    /**
     * 分页查询
     *
     * @param regionPageQueryReqDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<RegionDTO> getPage(RegionPageRequest regionPageQueryReqDTO) {
        Page<RegionDO> page = PageUtils.parsePageQuery(regionPageQueryReqDTO, RegionDO.class);
        Page<RegionDO> regionDOPage = baseMapper.selectPage(page, new QueryWrapper<>());
        List<RegionDTO> regionDTOS = regionConverter.regionDOsToRegionDTOs(regionDOPage.getRecords());
        return PageUtils.toPage(page, regionDTOS);
    }

    /**
     * 已开通服务区域列表
     *
     * @return 区域列表
     */
    @Override
    public List<RegionSimpleDTO> queryActiveRegionListDb() {
        LambdaQueryWrapper<RegionDO> queryWrapper = Wrappers.<RegionDO>lambdaQuery()
                .eq(RegionDO::getActiveStatus, HousekeepingStatusEnum.ENABLE.getStatus())
                .orderByAsc(RegionDO::getSortNum);
        List<RegionDO> regionDOList = baseMapper.selectList(queryWrapper);
        return regionConverter.regionDOsToRegionSimpleDTOs(regionDOList);
    }

    /**
     * 区域启用
     *
     * @param id 区域id
     */
    @Override
    @CachePut(cacheNames = RedisConstants.CacheName.REGION_CACHE, key = "'ACTIVE_REGIONS'")
    public List<RegionSimpleDTO> active(Long id) {
        //区域信息
        RegionDO regionDO = baseMapper.selectById(id);
        if(ObjectUtil.isNull(regionDO)){
            throw new ForbiddenOperationException("区域不存在");
        }

        //草稿或禁用状态方可启用
        Integer activeStatus = regionDO.getActiveStatus();
        if(!(HousekeepingStatusEnum.INIT.getStatus() == activeStatus || HousekeepingStatusEnum.DISABLE.getStatus() == activeStatus)){
            throw new ForbiddenOperationException("草稿或禁用状态方可启用");
        }

        //更新启用状态
        LambdaUpdateWrapper<RegionDO> updateWrapper = Wrappers.<RegionDO>lambdaUpdate()
                .eq(RegionDO::getId, id)
                .set(RegionDO::getActiveStatus, HousekeepingStatusEnum.ENABLE.getStatus());
        update(updateWrapper);

        return queryActiveRegionListDb();
    }

    /**
     * 区域禁用
     *
     * @param id 区域id
     */
    @Override
    @CachePut(cacheNames = RedisConstants.CacheName.REGION_CACHE, key = "'ACTIVE_REGIONS'")
    public List<RegionSimpleDTO> deactivate(Long id) {
        //区域信息
        RegionDO regionDO = baseMapper.selectById(id);
        if(ObjectUtil.isNull(regionDO)){
            throw new ForbiddenOperationException("区域不存在");
        }

        //启用状态方可禁用
        Integer activeStatus = regionDO.getActiveStatus();
        if(!(HousekeepingStatusEnum.ENABLE.getStatus() == activeStatus)){
            throw new ForbiddenOperationException("启用状态方可禁用");
        }

        //如果禁用区域下有上架的服务则无法禁用
        LambdaQueryWrapper<RegionServeDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RegionServeDO::getRegionId, id);
        Long count = regionServeService.count(queryWrapper);
        if(count > 0){
            throw new ForbiddenOperationException("禁用失败，该区域下有有上架的服务则无法禁用");
        }

        //更新禁用状态
        LambdaUpdateWrapper<RegionDO> updateWrapper = Wrappers.<RegionDO>lambdaUpdate()
                .eq(RegionDO::getId, id)
                .set(RegionDO::getActiveStatus, HousekeepingStatusEnum.DISABLE.getStatus());
        update(updateWrapper);

        return queryActiveRegionListDb();
    }

    /**
     * 已开通服务区域列表
     *
     * @return 区域简略列表
     */
    @Override
    @Cacheable(cacheNames = RedisConstants.CacheName.REGION_CACHE, key = "'ACTIVE_REGIONS'")
    public List<RegionSimpleDTO> queryActiveRegionListCache() {
        return regionService.queryActiveRegionListDb();
    }


    @Autowired
    RegionServeConverter converter;

    /**
     * 刷新区域id相关缓存：首页图标、热门服务、服务分类
     *
     * @param regionId 区域id
     */
    @Override
    public void refreshRegionRelateCaches(Long regionId) {

    }

}
