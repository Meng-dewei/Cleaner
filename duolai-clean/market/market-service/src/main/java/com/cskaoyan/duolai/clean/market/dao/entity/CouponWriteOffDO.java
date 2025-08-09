package com.cskaoyan.duolai.clean.market.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 优惠券核销表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("coupon_write_off")
public class CouponWriteOffDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    /**
     * 优惠券id
     */
    private Long couponId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 核销时使用的订单号
     */
    private Long ordersId;

    /**
     * 活动id
     */
    private Long activityId;

    /**
     * 核销时间
     */
    private LocalDateTime writeOffTime;

    /**
     * 核销人手机号
     */
    private String writeOffManPhone;

    /**
     * 核销人姓名
     */
    private String writeOffManName;


}
