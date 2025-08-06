package com.cskaoyan.duolai.clean.orders.utils;

import com.cskaoyan.duolai.clean.common.utils.DateUtils;
import com.cskaoyan.duolai.clean.common.utils.NumberUtils;

import java.time.LocalDateTime;

public class ServeTimeUtils {

    /**
     * 获取服务时间，用来处理抢单和派单的时间冲突问题
     *
     * @param serveStartTime
     * @return
     */
    public static long getServeTimeLong(LocalDateTime serveStartTime) {
        return NumberUtils.parseLong(DateUtils.format(serveStartTime, "yyyyMMddHHmm"));
    }
}
