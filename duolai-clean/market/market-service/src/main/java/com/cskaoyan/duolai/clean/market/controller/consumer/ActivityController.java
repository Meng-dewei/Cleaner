package com.cskaoyan.duolai.clean.market.controller.consumer;

import com.cskaoyan.duolai.clean.market.dto.SeizeCouponInfoDTO;
import com.cskaoyan.duolai.clean.market.service.IActivityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController("consumerActivityController")
@RequestMapping("/consumer/activity")
@Api(tags = "用户端-活动相关接口")
public class ActivityController {

    @Resource
    private IActivityService activityService;



    @GetMapping("/list")
    @ApiOperation("用户端抢券列表查询接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tabType", value = "页面tab类型，1：疯抢中，2：即将开始", required = true, dataTypeClass = Integer.class)})
    public List<SeizeCouponInfoDTO> queryForPage(@RequestParam(value = "tabType",required = true) Integer tabType) {
        return activityService.queryForListFromCache(tabType);
    }

}
