package com.cskaoyan.duolai.clean.market.controller.operation;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.market.request.ActivityPageRequest;
import com.cskaoyan.duolai.clean.market.request.ActivityCommand;
import com.cskaoyan.duolai.clean.market.dto.ActivityDTO;
import com.cskaoyan.duolai.clean.market.dto.ActivityInfoDTO;
import com.cskaoyan.duolai.clean.market.service.IActivityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("operationActivityController")
@RequestMapping("/operation/activity")
@Api(tags = "运营端-活动相关接口")
public class ActivityController {

    @Resource
    private IActivityService activityService;

    @GetMapping("/page")
    @ApiOperation("运营端分页查询活动")
    public PageDTO<ActivityDTO> queryForPage(ActivityPageRequest activityPageRequest) {
        return activityService.queryForPage(activityPageRequest);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询活动详情")
    @ApiImplicitParam(name = "id", value = "活动id", required = true, dataTypeClass = Long.class)
    public ActivityInfoDTO getDetail(@PathVariable("id") Long id) {
        return activityService.queryById(id);
    }

    @PostMapping("/save")
    @ApiOperation("活动保存")
    public void save(@RequestBody ActivityCommand activityCommand) {
        activityService.save(activityCommand);
    }

    @PostMapping("/revoke/{id}")
    @ApiOperation("活动撤销")
    @ApiImplicitParam(name = "id", value = "活动id", required = true, dataTypeClass = Long.class)
    public void revoke(@PathVariable("id") Long id) {
        activityService.revoke(id);
    }


}
