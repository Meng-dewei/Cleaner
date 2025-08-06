package com.cskaoyan.duolai.clean.pay.annotation;


import com.cskaoyan.duolai.clean.pay.enums.PayChannelEnum;

import java.lang.annotation.*;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented //标记注解
public @interface PayChannel {

    PayChannelEnum type();

}
