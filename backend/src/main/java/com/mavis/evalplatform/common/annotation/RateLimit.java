package com.mavis.evalplatform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * <p>
 * 基于 Redis + AOP 实现,默认按方法 IP 限流(对齐架构设计说明书 §8.2 限流措施:单 IP 60 次/分钟)。
 * <p>
 * 用法:
 * <pre>
 * &#64;RateLimit(limitPerMinute = 10)
 * public Result&lt;LoginResponse&gt; login(...) { ... }
 * </pre>
 *
 * @author 刘家豪
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 每分钟最大请求数(默认 60) */
    int limitPerMinute() default 60;

    /** 限流 key 前缀 */
    String key() default "";

    /** 是否按方法签名区分 key(默认 true) */
    boolean byMethod() default true;
}
