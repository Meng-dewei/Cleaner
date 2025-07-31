package com.cskaoyan.duolai.clean.housekeeping.controller.user;


import com.cskaoyan.duolai.clean.housekeeping.dto.SearchRegionServeDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.DisplayServeTypeDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.RegionServeDetailDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeTypeHomeDTO;
import com.cskaoyan.duolai.clean.housekeeping.service.IFirstPageService;
import com.cskaoyan.duolai.clean.housekeeping.service.IHomeService;
import com.cskaoyan.duolai.clean.housekeeping.service.IRegionServeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/customer/serve")
public class FirstPageServeController {
    @Resource
    private IRegionServeService serveService;
    @Resource
    private IFirstPageService IFirstPageService;
    @Resource
    private IHomeService iHomeService;

    /**
     * 首页服务列表
     */
    @GetMapping("/firstPageServeList")
    public List<ServeTypeHomeDTO> serveCategory(@RequestParam("regionId") Long regionId) {
        return iHomeService.queryServeIconCategoryByRegionIdCache(regionId);
    }

    /**
     * 首页热门服务列表
     */
    @GetMapping("/hotServeList")
    public List<RegionServeDetailDTO> listHotServe(@RequestParam("regionId") Long regionId) {
        return iHomeService.findHotServeListByRegionIdCache(regionId);
    }

    /*
       服务分类列表
     */
    @GetMapping("/serveTypeList")
    public List<DisplayServeTypeDTO> serveTypeList(@RequestParam("regionId") Long regionId) {
        return iHomeService.queryServeTypeListByRegionIdCache(regionId);
    }

    /*
       首页服务搜索
     */
    @GetMapping("/search")
    public List<SearchRegionServeDTO> findServeList(@RequestParam("cityCode") String cityCode,
                                                    @RequestParam(value = "serveTypeId", required = false) Long serveTypeId,
                                                    @RequestParam(value = "keyword", required = false) String keyword) {
        return IFirstPageService.findServeList(cityCode, serveTypeId, keyword);
    }

    /*
       根据id查询服务
     */
    @GetMapping("/{id}")
    public RegionServeDetailDTO findById(@PathVariable("id") Long id) {
        return serveService.findDetailByIdCache(id);
    }
}
