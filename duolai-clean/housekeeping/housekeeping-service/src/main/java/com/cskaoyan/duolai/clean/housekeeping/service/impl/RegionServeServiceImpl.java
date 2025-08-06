package com.cskaoyan.duolai.clean.housekeeping.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.expcetions.ForbiddenOperationException;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.housekeeping.converter.RegionServeConverter;
import com.cskaoyan.duolai.clean.housekeeping.dto.*;
import com.cskaoyan.duolai.clean.housekeeping.enums.HousekeepingStatusEnum;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.*;
import com.cskaoyan.duolai.clean.housekeeping.dao.mapper.*;
import com.cskaoyan.duolai.clean.housekeeping.request.RegionServeCommand;
import com.cskaoyan.duolai.clean.housekeeping.request.ServePageRequest;
import com.cskaoyan.duolai.clean.housekeeping.service.IHomeService;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionServeService;
import com.cskaoyan.duolai.clean.mysql.utils.PageUtils;
import com.cskaoyan.duolai.clean.redis.constants.RedisConstants;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务表 服务实现类
 * </p>
 */
@Service
public class RegionServeServiceImpl extends ServiceImpl<RegionServeMapper, RegionServeDO> implements IRegionServeService {

    @Resource
    RegionServeConverter regionServeConverter;

    @Resource
    ServeItemMapper serveItemMapper;

    @Resource
    RegionServeMapper regionServeMapper;

    @Resource
    RegionMapper regionMapper;
    @Resource
    ServeTypeMapper serveTypeMapper;

    @Resource
    ServeSyncMapper serveSyncMapper;


    @Override
    public PageDTO<RegionServeDTO> getPage(ServePageRequest servePageQueryReqDTO) {
        Page<RegionServeDO> regionServe2Page = PageUtils.parsePageQuery(servePageQueryReqDTO, RegionServeDO.class);
        Page<RegionServeDO> regionServeDOPage = regionServeMapper.queryRegionServeListByRegionId(servePageQueryReqDTO.getRegionId(), regionServe2Page);
        if (PageUtils.isEmpty(regionServeDOPage))
        {
            throw new ForbiddenOperationException("数据为空");
        }
        List<RegionServeDTO> dtos = regionServeConverter.regionServeDOsToRegionServeDTOs(regionServeDOPage.getRecords());
        return PageUtils.toPage(regionServeDOPage, dtos);
    }


    @Override
    public void batchAdd(List<RegionServeCommand> regionServeCommandList) {
        for (RegionServeCommand regionServeCommand : regionServeCommandList) {
            LambdaQueryWrapper<RegionDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RegionDO::getId, regionServeCommand.getRegionId());

            RegionDO regionDO = regionMapper.selectOne(queryWrapper);
            String cityCode = regionDO.getCityCode();
            RegionServeDO regionServeDO = new RegionServeDO();
            regionServeDO.setServeItemId(regionServeCommand.getServeItemId());
            regionServeDO.setRegionId(regionServeCommand.getRegionId());
            regionServeDO.setCityCode(cityCode);
            regionServeDO.setPrice(regionServeCommand.getPrice());

            baseMapper.insert(regionServeDO);
        }
    }

    @Override
    @Caching(
            put = {@CachePut(cacheNames = RedisConstants.CacheName.REGION_SERVE_DETAIL, key = "#id")},
            evict = {@CacheEvict(cacheNames = RedisConstants.CacheName.FIRST_PAGE_HOT_SERVE, key = "#regionId")}
    )
    public RegionServeDetailDTO updatePrice(Long id, Long regionId, BigDecimal price) {
        LambdaUpdateWrapper<RegionServeDO> updateWrapper = Wrappers.<RegionServeDO>lambdaUpdate()
                .eq(RegionServeDO::getId, id)
//                .eq(RegionServeDO::getRegionId, regionId)
                .set(RegionServeDO::getPrice, price);
        update(updateWrapper);

        // 更新价格
        RegionServeSyncDO regionServeSyncDO = new RegionServeSyncDO();
        regionServeSyncDO.setId(id);
        regionServeSyncDO.setPrice(price);
        serveSyncMapper.updateById(regionServeSyncDO);

        return findDetailByIdDb(id);
    }

    @Override
    @CachePut(cacheNames = RedisConstants.CacheName.FIRST_PAGE_HOT_SERVE, key = "#regionId")
    public List<RegionServeDetailDTO> changeHotStatus(Long id, Long regionId, Integer flag) {
        LambdaUpdateWrapper<RegionServeDO> updateWrapper = Wrappers.<RegionServeDO>lambdaUpdate()
                .eq(RegionServeDO::getId, id)
                .eq(RegionServeDO::getRegionId, regionId)
                .set(RegionServeDO::getIsHot, flag)
                .set(RegionServeDO::getHotTimeStamp, System.currentTimeMillis());
        update(updateWrapper);
        return findHotServeListByRegionId(regionId);
    }

    @Override
    public int queryServeCountByRegionIdAndSaleStatus(Long regionId, Integer saleStatus) {
        QueryWrapper<RegionServeDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("region_id", regionId);

        if (saleStatus != null) {
            queryWrapper.eq("sale_status", saleStatus);
        }

        return (int) count(queryWrapper);
    }

    @Override
    public int queryServeCountByServeItemIdAndSaleStatus(Long serveItemId, Integer saleStatus) {
        QueryWrapper<RegionServeDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("serve_item_id", serveItemId);

        if (saleStatus != null) {
            queryWrapper.eq("sale_status", saleStatus);
        }

        long count = count(queryWrapper);
        if (count > Integer.MAX_VALUE) {
            throw new ForbiddenOperationException("服务数量超过最大限制");
        }
        return (int) count;
    }

    @Override
    public void deleteById(Long id) {
        RegionServeDO regionServeDO = baseMapper.selectById(id);
        if(ObjectUtil.isNull(regionServeDO)){
            throw new ForbiddenOperationException("区域服务信息不存在");
        }

        Integer activeSataus = regionServeDO.getSaleStatus();
        if (HousekeepingStatusEnum.INIT.getStatus() != activeSataus) {
            throw new ForbiddenOperationException("草稿状态方可删除");
        }

        baseMapper.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = RedisConstants.CacheName.REGION_SERVE_DETAIL, key = "#id")
    public RegionServeDetailDTO findDetailByIdCache(Long id) {
        RegionServeDetailDTO detailByIdDb = findDetailByIdDb(id);
        if (detailByIdDb == null) {
            return new RegionServeDetailDTO();
        }

        return detailByIdDb;
    }

    public RegionServeDetailDTO findDetailByIdDb(Long id) {
        // 1. 查询区域服务项
        RegionServeDO regionServeDO = regionServeMapper.selectById(id);

        // 2. 判断区域的状态，如果使禁用状态，返回空集合
        RegionDO regionDO = regionMapper.selectById(regionServeDO.getRegionId());
        if (HousekeepingStatusEnum.DISABLE.getStatus() == regionDO.getActiveStatus()) {
            return new RegionServeDetailDTO();
        }

        // 3. 查询区域服务详情
        RegionServeDetailDO regionServeDetail = regionServeMapper.findRegionServeDetail(id);

        return regionServeConverter.regionServeDetailDO2DTO(regionServeDetail);
    }

    @Autowired
    IHomeService iHomeService;

    @Override
    @CachePut(cacheNames = RedisConstants.CacheName.FIRST_PAGE_PARTIAL_SERVE_CACHE, key = "#regionId")
    public List<ServeTypeHomeDTO> refreshFirstPageRegionServeList(Long regionId) {
        return iHomeService.queryServeIconCategoryByRegionIdDb(regionId);
    }

    @Override
    @CachePut(cacheNames = RedisConstants.CacheName.FIRST_PAGE_HOT_SERVE, key = "#regionId")
    public List<RegionServeDetailDTO> refreshFistPageHotServeList(Long regionId) {
        return findHotServeListByRegionId(regionId);
    }

    @Override
    @CachePut(cacheNames = RedisConstants.CacheName.REGION_SERVE_TYPE, key = "#regionId")
    public List<DisplayServeTypeDTO> refreshFirstPageServeTypeList(Long regionId) {
        return iHomeService.queryServeTypeListByRegionIdDb(regionId);
    }

    @Override
    public List<RegionServeDetailDTO> findHotServeListByRegionId(Long regionId) {
        // 1. 是否是已启用的区域
        RegionDO regionDO = regionMapper.selectById(regionId);
        if (HousekeepingStatusEnum.DISABLE.getStatus() == regionDO.getActiveStatus()) {
            return Collections.emptyList();
        }

        // 2. 查询精选推荐列表
        List<RegionServeDetailDO> hotServeListByRegionId = regionServeMapper.findHotServeListByRegionId(regionId);

        return regionServeConverter.regionServeDetailDOs2DTOs(hotServeListByRegionId);
    }

    @Override
    public List<DisplayServeTypeDTO> findServeTypeListByRegionId(Long regionId) {
        // 1. 是否是已启用的区域
        RegionDO regionDO = regionMapper.selectById(regionId);
        if (HousekeepingStatusEnum.DISABLE.getStatus() == regionDO.getActiveStatus()) {
            return Collections.emptyList();
        }

        // 2. 查询区域下的类型信息
        List<ServeTypeDO> serveTypeListByRegionId = regionServeMapper.findServeTypeListByRegionId(regionId);

        if (serveTypeListByRegionId == null) {
            return Collections.emptyList();
        }
        return regionServeConverter.serveTypeDOsToFirstPageServeTypeDTOs(serveTypeListByRegionId);
    }

    @Caching(
            put = {@CachePut(cacheNames = RedisConstants.CacheName.REGION_SERVE_DETAIL, key = "#id")},
            evict = {
                    @CacheEvict(cacheNames = RedisConstants.CacheName.FIRST_PAGE_PARTIAL_SERVE_CACHE, key = "#result.regionId"),
                    @CacheEvict(cacheNames = RedisConstants.CacheName.FIRST_PAGE_HOT_SERVE, key = "#result.regionId"),
                    @CacheEvict(cacheNames = RedisConstants.CacheName.REGION_SERVE_TYPE, key = "#result.regionId")
            }
    )
    @Override
    @Transactional
    public RegionServeDetailDTO onSale(Long id) {
        RegionServeDO regionServe = regionServeMapper.selectById(id);
        if (regionServe == null) {
            throw new ForbiddenOperationException("区域服务项不存在");
        }

        ServeDetailDO serveDetail = regionServeMapper.findServeDetailById(id);
        if (serveDetail == null) {
            throw new ForbiddenOperationException("服务详情不存在");
        }

        ServeItemDO serveItem = serveItemMapper.selectById(serveDetail.getServeItemId());
        if (serveItem == null || serveItem.getActiveStatus() != 2) {
            throw new ForbiddenOperationException("服务项未启用，无法上架");
        }

        ServeTypeDO serveType = serveTypeMapper.selectById(serveDetail.getServeTypeId());
        if (serveType == null || serveType.getActiveStatus() != 2) {
            throw new ForbiddenOperationException("服务类型未启用，无法上架");
        }

        RegionDO region = regionMapper.selectById(regionServe.getRegionId());
        if (region == null || region.getActiveStatus() != 2) {
            throw new ForbiddenOperationException("区域未启用，无法上架服务");
        }

        if (regionServe.getSaleStatus() != 0 && regionServe.getSaleStatus() != 1) {
            throw new ForbiddenOperationException("只有草稿或下架状态的区域服务项才能上架");
        }

        RegionServeDO updateEntity = new RegionServeDO();
        updateEntity.setId(id);
        updateEntity.setSaleStatus(2);
        regionServeMapper.updateById(updateEntity);

        addServeSync(id);
        return findDetailByIdDb(id);
    }

    private void addServeSync(Long serveId) {
        //服务信息
        RegionServeDO regionServe = baseMapper.selectById(serveId);
        //区域信息
        RegionDO region = regionMapper.selectById(regionServe.getRegionId());
        //服务项信息
        ServeItemDO serveItem = serveItemMapper.selectById(regionServe.getServeItemId());
        //服务类型
        ServeTypeDO serveType = serveTypeMapper.selectById(serveItem.getServeTypeId());

        RegionServeSyncDO regionServeSyncDO = regionServeConverter
                .regionServeToServeSyncDO(serveType, serveItem, regionServe, region.getCityCode());
        // 插入数据
        serveSyncMapper.insert(regionServeSyncDO);
    }


    @Caching(
            evict = {
                    @CacheEvict(cacheNames = RedisConstants.CacheName.REGION_SERVE_DETAIL, key = "#id"),
                    @CacheEvict(cacheNames = RedisConstants.CacheName.FIRST_PAGE_HOT_SERVE, key = "#result.regionId"),
                    @CacheEvict(cacheNames = RedisConstants.CacheName.FIRST_PAGE_PARTIAL_SERVE_CACHE, key = "#result.regionId"),
                    @CacheEvict(cacheNames = RedisConstants.CacheName.REGION_SERVE_TYPE, key = "#result.regionId")
            }
    )
    @Override
    @Transactional
    public RegionServeDTO offSale(Long id) {
        RegionServeDO regionServe = regionServeMapper.selectById(id);
        if (regionServe == null) {
            throw new ForbiddenOperationException("区域服务项不存在");
        }
        if (regionServe.getSaleStatus() != 2) {
            throw new ForbiddenOperationException("只有上架状态的区域服务项才能下架");
        }

        RegionServeDO updateEntity = new RegionServeDO();
        updateEntity.setId(id);
        updateEntity.setSaleStatus(1);

        regionServeMapper.updateById(updateEntity);

        RegionServeDO regionServeDO = baseMapper.selectById(id);
        serveSyncMapper.deleteById(id);
        return regionServeConverter.regionServeDOToRegionServeDTO(regionServeDO);
    }

    @Override
    public ServeDetailDTO findServeDetailById(Long id) {
        return null;
    }
}
