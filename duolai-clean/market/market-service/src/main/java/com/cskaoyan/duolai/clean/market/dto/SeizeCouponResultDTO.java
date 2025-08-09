package com.cskaoyan.duolai.clean.market.dto;

import lombok.Data;

@Data
public class SeizeCouponResultDTO {
    /*
     *  抢卷成功的用户id
     */
    Long userId;
    /*
     *  抢卷成功的活动id
     */
    Long activityId;
}
