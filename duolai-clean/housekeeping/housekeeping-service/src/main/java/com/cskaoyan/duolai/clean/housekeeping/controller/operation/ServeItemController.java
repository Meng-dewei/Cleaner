package com.cskaoyan.duolai.clean.housekeeping.controller.operation;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeItemDTO;
import com.cskaoyan.duolai.clean.housekeeping.request.ServeItemCommand;
import com.cskaoyan.duolai.clean.housekeeping.request.ServeItemPageRequest;
import com.cskaoyan.duolai.clean.housekeeping.service.IServeItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 服务表 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/operation/serve-item")
public class ServeItemController {
    @Resource
    private IServeItemService serveItemService;

    /*
       服务项新增
     */
    @PostMapping
    public void add(@RequestBody ServeItemCommand serveItemCommand) {
        serveItemService.addServeItem(serveItemCommand);
    }

    /*
       服务项修改
     */
    @PutMapping("/{id}")
    public void update(@PathVariable("id") Long id, @RequestBody ServeItemCommand serveItemCommand) {
        serveItemService.updateServeItem(id, serveItemCommand);
    }


    /*
        服务项启用
     */
    @PutMapping("/activate/{id}")    //启用状态，1：禁用，:2：启用
    public void activate(@PathVariable("id") Long id) {
        serveItemService.activateServeItem(id);
    }

    /*
       服务项禁用
     */
    @PutMapping("/deactivate/{id}")
    public void deactivate(@PathVariable("id") Long id) {
        serveItemService.deactivate(id);
    }

    /*
       服务项删除
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        serveItemService.deleteById(id);
    }

     /*
       服务项分页查询
     */
    @GetMapping("/page")
    public PageDTO<ServeItemDTO> page(ServeItemPageRequest serveItemPageQueryReqDTO) {
        return serveItemService.page(serveItemPageQueryReqDTO);
    }

    /*
       根据id查询服务项
     */
    @GetMapping("/{id}")
    public ServeItemDTO findById(@PathVariable("id") Long id) {
        return serveItemService.queryServeItemAndTypeById(id);
    }


}
