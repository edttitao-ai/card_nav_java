package com.tao.card_nav.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数级注解：标记"由 AOP 切面注入客户端 IP"的位置。
 *
 * <p>用于配合 {@link RateLimitedByIp}：
 * <ul>
 *   <li>原方法不需要自己解析 IP（AOP 切面提前解析好注入）</li>
 *   <li>fallback 方法也用这个注解 → AOP 切面把同一个 IP 传给 fallback</li>
 * </ul>
 *
 * <p>被标注的参数类型必须是 {@code String}。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimitKey {
}