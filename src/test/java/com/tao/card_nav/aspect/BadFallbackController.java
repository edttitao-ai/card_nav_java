package com.tao.card_nav.aspect;

import com.tao.card_nav.annotation.RateLimitKey;
import com.tao.card_nav.annotation.RateLimitedByIp;
import com.tao.card_nav.exception.RateLimitException;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

/**
 * {@link RateLimitedByIpAspectTest} 用的"坏签名"桩 controller：
 * fallback 缺少 +1 个 {@link RateLimitException} 参数。
 *
 * <p>顶级 public 类，理由同 {@link StubController}。
 */
public class BadFallbackController {

    @RateLimitedByIp(fallbackMethod = "rateLimitFallback")
    public Flux<String> chat(@RateLimitKey String clientIp) {
        return Flux.just("ok");
    }

    @SuppressWarnings("unused")
    private Flux<String> rateLimitFallback(String clientIp) {
        return Flux.just("bad");
    }

    public static final Method METHOD;
    public static final RateLimitedByIp ANNOTATION;
    static {
        try {
            METHOD = BadFallbackController.class.getDeclaredMethod("chat", String.class);
            ANNOTATION = METHOD.getAnnotation(RateLimitedByIp.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}