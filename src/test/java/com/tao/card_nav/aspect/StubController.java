package com.tao.card_nav.aspect;

import com.tao.card_nav.annotation.RateLimitKey;
import com.tao.card_nav.annotation.RateLimitedByIp;
import com.tao.card_nav.exception.RateLimitException;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

/**
 * {@link RateLimitedByIpAspectTest} 用的桩 controller。
 *
 * <p>必须是顶级 public 类（不是内部类），避免 javac 合成的桥接方法干扰反射查找。
 * 通过静态字段直接暴露方法 + 注解，测试里直接复用，不再反射查找。
 */
public class StubController {

    @RateLimitedByIp(fallbackMethod = "rateLimitFallback")
    public Flux<String> chat(@RateLimitKey String clientIp) {
        return Flux.just("ok-" + clientIp);
    }

    @SuppressWarnings("unused")
    private Flux<String> rateLimitFallback(String clientIp, RateLimitException ex) {
        return Flux.just("rate-limited-" + clientIp + "-" + ex.getRetryAfterSeconds());
    }

    public static final Method METHOD;
    public static final RateLimitedByIp ANNOTATION;
    static {
        try {
            METHOD = StubController.class.getDeclaredMethod("chat", String.class);
            ANNOTATION = METHOD.getAnnotation(RateLimitedByIp.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}