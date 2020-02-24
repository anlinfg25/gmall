package com.atguigu.gmall.index.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存key的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 缓存的过期时间：已分为单位
     * @return
     */
    int timeout() default 5;

    /**
     * 缓存的过期时间：已分为单位
     * 防止缓存雪崩，指定的随机值范围
     * @return
     */
    int random() default 5;
}
