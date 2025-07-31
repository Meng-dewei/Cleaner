package com.cskaoyan.duolai.clean.housekeeping.controller.operation;


import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.housekeeping.converter.RegionConverter;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionSimpleDTO;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.RegionDO;
import com.cskaoyan.duolai.clean.housekeeping.request.RegionCommand;
import com.cskaoyan.duolai.clean.housekeeping.request.RegionPageRequest;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionDTO;
import com.cskaoyan.duolai.clean.housekeeping.service.IHomeService;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 运营端 - 区域相关接口
 * </p>
 */
@RestController("OperationRegionController")
@RequestMapping("/operation/region")
public class RegionController {
    @Resource
    private IRegionService regionService;

    @Resource
    RegionConverter regionConverter;


    /*
        区域新增
     */
    @PostMapping
    public void addRegion(@RequestBody RegionCommand regionCommand) {
        regionService.addRegion(regionCommand);
    }

    /*
       区域修改
     */
    @PutMapping("/{id}")
    public void update(@PathVariable("id") Long id,
                       @RequestParam("managerName") String managerName,
                       @RequestParam("managerPhone") String managerPhone) {
        regionService.update(id, managerName, managerPhone);
    }

    /*
       区域删除
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        regionService.deleteById(id);
    }

    /*
        区域分页查询
     */
    @GetMapping("/page")
    public PageDTO<RegionDTO> getPage(RegionPageRequest regionPageQueryReqDTO) {
        return regionService.getPage(regionPageQueryReqDTO);
    }

    /*
       根据id查询
     */
    @GetMapping("/{id}")
    public RegionDTO findById(@PathVariable("id") Long id) {
        RegionDO regionDO = regionService.getById(id);
        RegionDTO regionDTO = regionConverter.regionDOToRegionDTO(regionDO);
        return regionDTO;
    }

    /*
      区域启用
     */
    @PutMapping("/activate/{id}")
    public void activate(@PathVariable("id") Long id) {
        regionService.active(id);
    }

    /*
    区域禁用
     */
    @PutMapping("/deactivate/{id}")
    public void deactivate(@PathVariable("id") Long id) {
        regionService.deactivate(id);
    }

    /*
       刷新区域相关缓存
     */
    @PutMapping("/refreshRegionRelateCaches/{id}")
    public void refreshRegionRelateCaches(@PathVariable("id") Long id) {
        regionService.refreshRegionRelateCaches(id);
    }
}
