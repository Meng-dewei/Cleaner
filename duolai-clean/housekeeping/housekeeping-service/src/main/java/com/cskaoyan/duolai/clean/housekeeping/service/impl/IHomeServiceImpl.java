package com.cskaoyan.duolai.clean.housekeeping.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.cskaoyan.duolai.clean.housekeeping.converter.RegionServeConverter;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.HomeRegionServeSimpleDO;
import com.cskaoyan.duolai.clean.housekeeping.dao.mapper.RegionMapper;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionSimpleDTO;
import com.cskaoyan.duolai.clean.housekeeping.enums.HousekeepingStatusEnum;
import com.cskaoyan.duolai.clean.housekeeping.dao.mapper.RegionServeMapper;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.RegionDO;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.ServeTypeHomeDO;
import com.cskaoyan.duolai.clean.housekeeping.dto.DisplayServeTypeDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionServeHomeDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionServeDetailDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeTypeHomeDTO;
import com.cskaoyan.duolai.clean.housekeeping.service.IHomeService;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionServeService;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionService;
import com.cskaoyan.duolai.clean.housekeeping.service.IServeItemService;
import com.cskaoyan.duolai.clean.redis.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.print.DocFlavor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 首页查询相关功能
 **/
@Slf4j
@Service
public class IHomeServiceImpl implements IHomeService {
    @Resource
    private IRegionService regionService;
    @Resource
    private IRegionServeService serveService;
    @Resource
    private RegionServeMapper serveMapper;
    @Resource
    RegionServeConverter regionServeConverter;
    @Autowired
    RegionMapper regionMapper;


    /**
     * 根据区域id获取服务图标信息
     *
     * @param regionId 区域id
     * @return 服务图标列表
     */
    @Override
    @Cacheable(cacheNames = RedisConstants.CacheName.FIRST_PAGE_PARTIAL_SERVE_CACHE, key = "#regionId")
    public List<ServeTypeHomeDTO> queryServeIconCategoryByRegionIdCache(Long regionId) {
        List<ServeTypeHomeDTO> serveTypeHomeDTOS = queryServeIconCategoryByRegionIdDb(regionId);
        if (serveTypeHomeDTOS == null) {
            return Collections.emptyList();
        }
        return serveTypeHomeDTOS;
    }

    // 从数据库中查询首页服务列表的方法
    public List<ServeTypeHomeDTO> queryServeIconCategoryByRegionIdDb(Long regionId) {
        // 1. 判断目标区域是否已启用
        RegionDO regionDO = regionMapper.selectById(regionId);
        if (HousekeepingStatusEnum.DISABLE.getStatus() == regionDO.getActiveStatus()) {
            // 返回空列表
            return Collections.emptyList();
        }

        // 2. 查询区域服务列表
        List<ServeTypeHomeDO> serveIconCategory = serveMapper.findServeIconCategoryByRegionId(regionId);

        // 3. 对数据库中的数据裁剪
        int serveTypeSize = serveIconCategory.size() > 2 ?  2 : serveIconCategory.size();
        List<ServeTypeHomeDO> serveTypeHomes = serveIconCategory.subList(0, serveTypeSize);
        // 3.1 裁剪类型
        List<ServeTypeHomeDO> resultTypes = new ArrayList<>(serveTypeHomes);
        resultTypes.forEach(serveTypeHomeDO -> {
            // 获取包含的服务项集合
            List<HomeRegionServeSimpleDO> serveResDTOList = serveTypeHomeDO.getServeResDTOList();

            // 裁剪服务项
            int serveItemSize = serveResDTOList.size() > 4 ? 4 : serveResDTOList.size();
            List<HomeRegionServeSimpleDO> itemReulst = new ArrayList<>(serveResDTOList.subList(0, serveItemSize));
            serveTypeHomeDO.setServeResDTOList(itemReulst);
        });

        return regionServeConverter.serveTypeHomeDOsToDTOs(resultTypes);
    }


    /**
     * 根据区域id查询热门服务列表
     *
     * @param regionId 区域id
     * @return 服务列表
     */
    @Override
    @Cacheable(cacheNames = RedisConstants.CacheName.FIRST_PAGE_HOT_SERVE, key = "#regionId")
    public List<RegionServeDetailDTO> findHotServeListByRegionIdCache(Long regionId) {
        List<RegionServeDetailDTO> hotServeListByRegionId = serveService.findHotServeListByRegionId(regionId);

        if (hotServeListByRegionId == null) {
            return Collections.emptyList();
        }

        return hotServeListByRegionId;
    }


    /**
     * 根据区域id查询已开通的服务类型
     *
     * @param regionId 区域id
     * @return 已开通的服务类型
     */
    @Override
    @Cacheable(cacheNames = RedisConstants.CacheName.REGION_SERVE_TYPE, key = "#regionId")
    public List<DisplayServeTypeDTO> queryServeTypeListByRegionIdCache(Long regionId) {
        List<DisplayServeTypeDTO> displayServeTypeDTOS = queryServeTypeListByRegionIdDb(regionId);
        return displayServeTypeDTOS;
    }

    public List<DisplayServeTypeDTO> queryServeTypeListByRegionIdDb(Long regionId) {
        return serveService.findServeTypeListByRegionId(regionId);
    }

}
