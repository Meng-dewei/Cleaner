package com.cskaoyan.duolai.clean.market.converter;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.market.dao.entity.ActivityDO;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponDO;
import com.cskaoyan.duolai.clean.market.dto.ActivityInfoDTO;
import com.cskaoyan.duolai.clean.market.dto.CouponDTO;
import com.cskaoyan.duolai.clean.market.dto.SeizeCouponInfoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CouponConverter {

    CouponDTO couponToCouponInfoDTO(CouponDO counpon);

    List<CouponDTO> couponsToCouponInfoDTOs(List<CouponDO> couponDO);

    AvailableCouponsDTO couponDoToAvailableCouponsDTO(CouponDO couponDO);

    @Mapping(source = "list", target = "list")
    @Mapping(source = "total", target = "total")
    @Mapping(source = "pages", target = "pages")
    PageDTO<CouponDTO> toCouponInfoResPage(List<CouponDO> list, Integer total, Integer pages);

   SeizeCouponInfoDTO activityDOToSeizeCouponDTO(ActivityDO activityDO);

   ActivityInfoDTO seizeCouponInfoDTOToActivityInfoDTO(SeizeCouponInfoDTO seizeCouponInfoDTO);

   List<SeizeCouponInfoDTO> activityDOsToSeizeCouponDTOs(List<ActivityDO> activityDO);


    List<ActivityInfoDTO> seizeCouponInfoDTOsToActivityInfoDTOs(List<SeizeCouponInfoDTO> seizeCouponInfoDTOs);
}
