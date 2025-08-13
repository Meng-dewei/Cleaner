package com.cskaoyan.duolai.clean.market.converter;


import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.market.dao.entity.ActivityDO;
import com.cskaoyan.duolai.clean.market.request.ActivityCommand;
import com.cskaoyan.duolai.clean.market.dto.ActivityDTO;
import com.cskaoyan.duolai.clean.market.dto.ActivityInfoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ActivityConverter {


//   @Mapping(source = "totalNum", target = "")
   ActivityDO activityCommandToActivityDO(ActivityCommand activityCommand);
    ActivityInfoDTO activityToActivityInfoDTO(ActivityDO activityDO);

    ActivityDTO activityToActivityDTO(ActivityDO activityDO);

    @Mapping(source = "list", target = "list")
    @Mapping(source = "total", target = "total")
    @Mapping(source = "pages", target = "pages")
    PageDTO<ActivityDTO> toActivityInfoResPage(List<ActivityDO> list, Integer total, Integer pages);


}
