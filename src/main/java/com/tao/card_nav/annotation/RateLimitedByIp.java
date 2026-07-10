package com.tao.card_nav.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级限流注解：按调用方客户端 IP 限流。
 *
 * <p>用法（典型场景 - controller 方法）：
 * <pre>
 * {@code
 * @PostMapping
 * @RateLimitedByIp(fallbackMethod = "rateLimitFallback")
 * public Flux<ServerSentEvent<String>> chat(
 *         @RequestBody Map<String, String> payload,
 *         HttpServletRequest request,
 *         @RateLimitKey String clientIp) {
 *     // 业务逻辑
 * }
 *
 * // fallback 方法签名：与原方法参数列表一致 + 末尾追加 RateLimitException
 * private Flux<ServerSentEvent<String>> rateLimitFallback(
 *         Map<String, String> payload, HttpServletRequest request,
 *         String clientIp, RateLimitException ex) {
 *     // 限流时的降级响应
 * }
 * }
 * </pre>
 *
 * <p>为什么不用 Resilience4j 标准 {@code @RateLimiter}？标准注解的 name 是静态配置，
 * 所有调用共享一个 RateLimiter 实例，无法做到"按 IP 各自一个 bucket"。本注解由
 * {@code RateLimitedByIpAspect} 在 AOP 层动态按 IP 取 RateLimiter，兼顾注解式写法
 * 与按 key 隔离。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimitedByIp {

    /**
     * 限流触发时调用的降级方法名。
     *
     * <p>该方法必须与原方法在同一类内，参数列表与原方法一致，末尾追加
     * {@link com.tao.card_nav.exception.RateLimitException} 参数。
     */
    String fallbackMethod();
}