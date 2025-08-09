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
 * 优惠券使用回退记录
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("coupon_use_back")
public class CouponUseBackDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回退记录id
     */
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
     * 回退时间
     */
    private LocalDateTime useBackTime;

    /**
     * 核销时间
     */
    private LocalDateTime writeOffTime;


}
