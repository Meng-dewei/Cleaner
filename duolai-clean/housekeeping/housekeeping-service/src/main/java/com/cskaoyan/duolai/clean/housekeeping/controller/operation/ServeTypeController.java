package com.cskaoyan.duolai.clean.housekeeping.controller.operation;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeTypeSimpleDTO;
import com.cskaoyan.duolai.clean.housekeeping.request.ServeTypeCommand;
import com.cskaoyan.duolai.clean.housekeeping.request.ServeTypePageRequest;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeTypeDTO;
import com.cskaoyan.duolai.clean.housekeeping.service.IServeTypeService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务类型相关接口
 **/
@RestController("OperationServeTypeController")
@RequestMapping("/operation/serve-type")
public class ServeTypeController {
    @Resource
    private IServeTypeService serveTypeService;

    /*
       返回启用的服务类型列表
     */
    @GetMapping("active/list")
    public List<ServeTypeSimpleDTO> activeList() {
        // 2 为激活状态
        return serveTypeService.activeList(2);
    }

    /*
       服务类型新增
     */
    @PostMapping
    public void add(@RequestBody ServeTypeCommand serveTypeCommand) {
        serveTypeService.addServeType(serveTypeCommand);
    }

    /*
        服务类型修改
     */
    @PutMapping("/{id}")
    public void update( @PathVariable("id") Long id,
                       @RequestBody ServeTypeCommand serveTypeCommand) {
        serveTypeService.updateServeType(id, serveTypeCommand);
    }

    /*
      服务类型启用
     */
    @PutMapping("/activate/{id}")
    public void activate(@PathVariable("id") Long id) {
        serveTypeService.activateServeType(id);
    }

    /*
        服务类型禁用
     */
    @PutMapping("/deactivate/{id}")
    public void deactivate(@PathVariable("id") Long id) {
        serveTypeService.deactivateServeType(id);
    }

    /*
       服务类型删除
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        serveTypeService.deleteById(id);
    }

    /*
        服务类型分页查询
     */
    @GetMapping("/page")
    public PageDTO<ServeTypeDTO> page(ServeTypePageRequest serveTypePageRequest) {
        return serveTypeService.getPage(serveTypePageRequest);
    }
}
