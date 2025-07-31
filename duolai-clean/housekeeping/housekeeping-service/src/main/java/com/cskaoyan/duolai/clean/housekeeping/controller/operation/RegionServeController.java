package com.cskaoyan.duolai.clean.housekeeping.controller.operation;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.RegionServeDO;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionServeDetailDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeDetailDTO;
import com.cskaoyan.duolai.clean.housekeeping.enums.HousekeepingStatusEnum;
import com.cskaoyan.duolai.clean.housekeeping.request.RegionServeCommand;
import com.cskaoyan.duolai.clean.housekeeping.request.ServePageRequest;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionServeDTO;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionServeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.wildfly.common.annotation.NotNull;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * 运营端 - 区域服务相关接口
 */
@RestController
@RequestMapping("/operation/serve")
public class RegionServeController {

    @Resource
    private IRegionServeService serveService;


    /*
      区域服务分页查询
     */
    @GetMapping("/page")
    public PageDTO<RegionServeDTO> getPage(ServePageRequest servePageQueryReqDTO) {
        PageDTO<RegionServeDTO> page = serveService.getPage(servePageQueryReqDTO);
        return page;
    }

    /*
        区域服务批量新增
     */
    @PostMapping("/batch")
    public void add(@RequestBody List<RegionServeCommand> regionServeCommandList) {
        serveService.batchAdd(regionServeCommandList);
    }

    /*
        区域服务价格修改
     */
    @PutMapping("/{id}")
    public void updatePrice(@PathVariable("id") Long id,
                           @RequestParam("price") Double price) {
        RegionServeDO byId = serveService.getById(id);
        serveService.updatePrice(id, byId.getRegionId(), BigDecimal.valueOf(price));
    }

    /*
        区域服务删除
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        serveService.deleteById(id);
    }

    /*
        区域服务上架
     */
    @PutMapping("/onSale/{id}")
    public void onSale(@PathVariable("id") Long id) {
        serveService.onSale(id);
    }

    /*
        区域服务下架
     */
    @PutMapping("/offSale/{id}")
    public void offSale(@PathVariable("id") Long id) {
        serveService.offSale(id);
    }

    /*
      区域服务设置热门
     */
    @PutMapping("/onHot/{id}")
    public void onHot(@NotNull() @PathVariable("id") Long id) {
        RegionServeDO byId = serveService.getById(id);
        serveService.changeHotStatus(id, byId.getRegionId(), HousekeepingStatusEnum.HOT.getStatus());
    }

    /*
       区域服务取消热门
     */
    @PutMapping("/offHot/{id}")
    public void offHot(@NotNull() @PathVariable("id") Long id) {
        RegionServeDO byId = serveService.getById(id);
        serveService.changeHotStatus(id, byId.getRegionId(), HousekeepingStatusEnum.NOT_HOT.getStatus());
    }


}
