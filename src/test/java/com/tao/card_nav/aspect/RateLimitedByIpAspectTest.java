package com.tao.card_nav.aspect;

import com.tao.card_nav.annotation.RateLimitKey;
import com.tao.card_nav.annotation.RateLimitedByIp;
import com.tao.card_nav.exception.RateLimitException;
import com.tao.card_nav.service.RateLimitService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitedByIpAspect 单元测试。
 *
 * <p>核心目标：
 * <ol>
 *   <li>正常请求：把 IP 注入到 @RateLimitKey 参数，调 proceed</li>
 *   <li>触发限流：不调 proceed，反射调 fallback，传递 RateLimitException</li>
 *   <li>fallback 签名错：抛 IllegalStateException</li>
 * </ol>
 *
 * <p>不需要启动 Spring 容器 —— 切面的纯逻辑（参数注入、fallback 反射调用）可单测；
 * 真实 IP 解析在 {@code ClientIpUtilsTest} 覆盖。
 *
 * <p>设计说明：StubController 是顶级 public 类，避免内部类编译期合成方法导致
 * 反射查找失败。{@code getAnnotation} 复用 {@code mockPjp} 里已经拿到的 Method
 * 直接 {@code getAnnotation(...)}，不再二次反射查找。
 */
class RateLimitedByIpAspectTest {

    private RateLimitService rateLimitService;
    private RateLimitedByIpAspect aspect;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        rateLimitService = mock(RateLimitService.class);
        meterRegistry = new SimpleMeterRegistry();
        aspect = new RateLimitedByIpAspect(rateLimitService, meterRegistry);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @SuppressWarnings("unchecked")
    void around_normalRequest_injectsIpAndProceeds() throws Throwable {
        bindRequest("203.0.113.7");
        StubController target = new StubController();
        ProceedingJoinPoint pjp = mockPjp(target, new Object[]{null});

        doNothing().when(rateLimitService).acquireOrThrow(anyString());
        when(pjp.proceed(new Object[]{"203.0.113.7"})).thenReturn(Flux.just("ok-203.0.113.7"));

        Object result = aspect.around(pjp);

        verify(rateLimitService, times(1)).acquireOrThrow("203.0.113.7");
        verify(pjp, times(1)).proceed(new Object[]{"203.0.113.7"});
        List<String> items = ((Flux<String>) result).collectList().block();
        assertThat(items).containsExactly("ok-203.0.113.7");
    }

    @Test
    @SuppressWarnings("unchecked")
    void around_rateLimited_invokesFallbackInsteadOfProceed() throws Throwable {
        bindRequest("198.51.100.10");
        StubController target = new StubController();
        ProceedingJoinPoint pjp = mockPjp(target, new Object[]{null});

        doThrow(new RateLimitException("198.51.100.10", 60L))
                .when(rateLimitService).acquireOrThrow("198.51.100.10");

        Object result = aspect.around(pjp);

        verify(pjp, never()).proceed();
        List<String> items = ((Flux<String>) result).collectList().block();
        assertThat(items).containsExactly("rate-limited-198.51.100.10-60");
    }

    @Test
    void around_fallbackSignatureMismatch_throwsIllegalState() throws Throwable {
        bindRequest("10.0.0.1");
        BadFallbackController target = new BadFallbackController();
        ProceedingJoinPoint pjp = mockPjp(target, new Object[]{null});

        doThrow(new RateLimitException("10.0.0.1", 30L))
                .when(rateLimitService).acquireOrThrow("10.0.0.1");

        assertThatThrownBy(() -> aspect.around(pjp))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rateLimitFallback");
    }

    @Test
    @SuppressWarnings("unchecked")
    void around_requestAttributesMissing_usesUnknown() throws Throwable {
        StubController target = new StubController();
        ProceedingJoinPoint pjp = mockPjp(target, new Object[]{null});
        doNothing().when(rateLimitService).acquireOrThrow("unknown");
        when(pjp.proceed(new Object[]{"unknown"})).thenReturn(Flux.just("ok-unknown"));

        Object result = aspect.around(pjp);

        verify(rateLimitService, times(1)).acquireOrThrow("unknown");
        verify(pjp, times(1)).proceed(new Object[]{"unknown"});
        List<String> items = ((Flux<String>) result).collectList().block();
        assertThat(items).containsExactly("ok-unknown");
    }

    // ===== helpers =====

    private void bindRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /**
     * 构造 ProceedingJoinPoint mock：通过 StubController.ANNOTATION 拿到方法，
     * 直接把 Method 注入到 MethodSignature mock，避免二次反射查找。
     */
    private ProceedingJoinPoint mockPjp(Object target, Object[] args) {
        MethodSignature signature;
        if (target instanceof StubController) {
            signature = mockSignature(StubController.METHOD);
        } else if (target instanceof BadFallbackController) {
            signature = mockSignature(BadFallbackController.METHOD);
        } else {
            throw new IllegalArgumentException("unknown target: " + target);
        }

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(args);
        when(pjp.getTarget()).thenReturn(target);
        return pjp;
    }

    private MethodSignature mockSignature(java.lang.reflect.Method method) {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        return signature;
    }
}