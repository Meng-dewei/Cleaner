package com.cskaoyan.duolai.clean.orders.dispatch.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("服务开始模型")
public class ServeFinishedCommand {
    @ApiModelProperty("服务id")
    private Long id;

    @ApiModelProperty("服务完成图片")
    private List<String> serveAfterImgs;

    @ApiModelProperty("服务完成说明")
    private String serveAfterIllustrate;
}
