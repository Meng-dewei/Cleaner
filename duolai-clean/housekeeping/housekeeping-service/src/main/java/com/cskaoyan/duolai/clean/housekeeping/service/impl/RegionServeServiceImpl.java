package com.cskaoyan.duolai.clean.housekeeping.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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

        return null;
    }



    @Override
    public void batchAdd(List<RegionServeCommand> regionServeCommandList) {

    }

    @Override
    public RegionServeDetailDTO updatePrice(Long id, Long regionId, BigDecimal price) {

        return null;
    }

    @Override
    public List<RegionServeDetailDTO> changeHotStatus(Long id, Long regionId,  Integer flag) {

        return null;
    }

    @Override
    public int queryServeCountByRegionIdAndSaleStatus(Long regionId, Integer saleStatus) {

        return 0;
    }

    @Override
    public int queryServeCountByServeItemIdAndSaleStatus(Long serveItemId, Integer saleStatus) {

        return 0;
    }

    @Override
    public void deleteById(Long id) {
    }

    @Override
    public RegionServeDetailDTO findDetailByIdCache(Long id) {
        return null;
    }

    public RegionServeDetailDTO findDetailByIdDb(Long id) {
      return null;
    }

    @Autowired
    IHomeService iHomeService;

    @Override
    public List<ServeTypeHomeDTO> refreshFirstPageRegionServeList(Long regionId) {
        return iHomeService.queryServeIconCategoryByRegionIdDb(regionId);
    }

    @Override
    public List<RegionServeDetailDTO> refreshFistPageHotServeList(Long regionId) {
        return findHotServeListByRegionId(regionId);
    }

    @Override
    public List<DisplayServeTypeDTO> refreshFirstPageServeTypeList(Long regionId) {
        return iHomeService.queryServeTypeListByRegionIdDb(regionId);
    }

    @Override
    public List<RegionServeDetailDTO> findHotServeListByRegionId(Long regionId) {
        return null;
    }

    @Override
    public List<DisplayServeTypeDTO> findServeTypeListByRegionId(Long regionId) {
        return null;
    }

    @Override
    @Transactional
    public RegionServeDetailDTO onSale(Long id) {


        return findDetailByIdDb(id);
    }


    @Override
    @Transactional
    public RegionServeDTO offSale(Long id) {


        return null;
    }

    @Override
    public ServeDetailDTO findServeDetailById(Long id) {
         return null;
    }
}
