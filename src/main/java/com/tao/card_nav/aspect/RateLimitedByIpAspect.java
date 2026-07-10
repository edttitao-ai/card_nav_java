package com.tao.card_nav.aspect;

import com.tao.card_nav.annotation.RateLimitKey;
import com.tao.card_nav.annotation.RateLimitedByIp;
import com.tao.card_nav.exception.RateLimitException;
import com.tao.card_nav.service.RateLimitService;
import com.tao.card_nav.util.ClientIpUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RateLimitedByIp} 注解的 AOP 切面。
 *
 * <p>职责：
 * <ol>
 *   <li>从当前 HTTP 请求中解析客户端 IP（{@link ClientIpUtils}）</li>
 *   <li>把 IP 注入到原方法被 {@link RateLimitKey} 标注的参数位置</li>
 *   <li>调 {@link RateLimitService#acquireOrThrow(String)} 扣令牌</li>
 *   <li>若触发限流：不调 proceed，改为反射调用 fallbackMethod，把
 *       {@link RateLimitException} 传给 fallback</li>
 * </ol>
 *
 * <p>为什么自造切面：Resilience4j 标准 {@code @RateLimiter} 的 name 是静态配置，
 * 所有调用共享一个 RateLimiter 实例，无法按 IP 隔离。本切面在 AOP 层动态取
 * RateLimiter（走 {@code RateLimitService} 内部的 {@code limiterCache}），
 * 既保持注解式写法的清爽，又保留"每 IP 各自 bucket"的隔离语义。
 *
 * <p>Aspect order 设为 {@link Ordered#HIGHEST_PRECEDENCE}，保证在其他 AOP
 * （如缓存、事件发布）之前先完成限流。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitedByIpAspect {

    /** 限流指标名（暴露后缀会被 Prometheus 加 _total） */
    static final String METRIC_REQUESTS = "rate_limit_requests";

    private final RateLimitService rateLimitService;
    private final MeterRegistry meterRegistry;

    @Around("@annotation(com.tao.card_nav.annotation.RateLimitedByIp)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // 从 method 上拿注解
        RateLimitedByIp rateLimitedByIp = method.getAnnotation(RateLimitedByIp.class);

        // 1. 解析客户端 IP
        String clientIp = resolveClientIp();

        // 2. 注入 @RateLimitKey 参数
        Object[] newArgs = injectRateLimitKey(pjp.getArgs(), method, clientIp);

        // 3. 调 RateLimitService 扣令牌
        try {
            rateLimitService.acquireOrThrow(clientIp);
            recordMetric(method, "allowed");
        } catch (RateLimitException rle) {
            // 限流触发：调 fallback 而非 proceed
            log.info("AI 限流触发 clientIp={} method={}", clientIp, method.getName());
            recordMetric(method, "limited");
            return invokeFallback(pjp, method, newArgs, rateLimitedByIp.fallbackMethod(), rle);
        }

        // 4. 正常放行
        return pjp.proceed(newArgs);
    }

    /**
     * 累加限流指标。tag 维度：
     * <ul>
     *   <li>method：被拦截的方法名（按 controller 维度）</li>
     *   <li>outcome：{@code allowed} / {@code limited}</li>
     * </ul>
     * 同一 metric name 的所有实例 tag key 集合必须一致；Micrometer 1.16 / Prometheus 1.4 都会校验。
     */
    private void recordMetric(Method method, String outcome) {
        Counter.builder(METRIC_REQUESTS)
                .description("@RateLimitedByIp 拦截的请求总数，按 outcome 区分是否命中限流")
                .tag("method", method.getName())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 从当前请求上下文解析客户端 IP。
     */
    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            // 非 Web 上下文（异步/测试场景）—— 走 unknown
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        return ClientIpUtils.resolve(request);
    }

    /**
     * 在原始参数列表里把 {@link RateLimitKey} 标注的位置替换为 clientIp。
     */
    private Object[] injectRateLimitKey(Object[] args, Method method, String clientIp) {
        if (args == null) {
            args = new Object[0];
        }
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Object[] result = args.clone();
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation ann : paramAnnotations[i]) {
                if (ann instanceof RateLimitKey) {
                    result[i] = clientIp;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 反射调用 fallbackMethod。
     *
     * <p>fallback 签名规则：与原方法参数列表一致，末尾追加一个
     * {@link RateLimitException} 参数。
     *
     * @return fallback 的返回值（强转由调用方负责）
     */
    private Object invokeFallback(ProceedingJoinPoint pjp, Method originalMethod, Object[] originalArgs,
                                  String fallbackMethodName, RateLimitException ex) throws Throwable {
        // 拿真实类（绕开 CGLIB 代理）
        Class<?> targetClass = AopUtils.getTargetClass(pjp.getTarget());
        Method fallback = findFallbackMethod(targetClass, originalMethod, fallbackMethodName);
        if (fallback == null) {
            throw new IllegalStateException(
                    "@RateLimitedByIp fallbackMethod 未找到或签名不匹配: "
                            + targetClass.getSimpleName() + "#" + fallbackMethodName
                            + "（原方法: " + originalMethod.getName() + "）");
        }

        // 构造 fallback 参数：原 args + RateLimitException
        List<Object> args = new ArrayList<>(originalArgs.length + 1);
        for (Object a : originalArgs) {
            args.add(a);
        }
        args.add(ex);

        try {
            fallback.setAccessible(true);
            return fallback.invoke(pjp.getTarget(), args.toArray());
        } catch (InvocationTargetException ite) {
            // fallback 自己抛异常 → 解包
            throw ite.getCause();
        }
    }

    /**
     * 在 targetClass 中查找 fallback：与原方法同名 + 参数列表 = 原方法参数列表 + RateLimitException。
     */
    private Method findFallbackMethod(Class<?> targetClass, Method originalMethod, String fallbackName) {
        for (Method m : targetClass.getDeclaredMethods()) {
            if (!m.getName().equals(fallbackName)) {
                continue;
            }
            Class<?>[] originalParamTypes = originalMethod.getParameterTypes();
            Class<?>[] fallbackParamTypes = m.getParameterTypes();
            if (fallbackParamTypes.length != originalParamTypes.length + 1) {
                continue;
            }
            // 前 N 个参数类型必须一致
            boolean same = true;
            for (int i = 0; i < originalParamTypes.length; i++) {
                if (!originalParamTypes[i].equals(fallbackParamTypes[i])) {
                    same = false;
                    break;
                }
            }
            if (!same) {
                continue;
            }
            // 最后一个参数必须是 RateLimitException（或其父类）
            if (RateLimitException.class.isAssignableFrom(fallbackParamTypes[fallbackParamTypes.length - 1])) {
                return m;
            }
        }
        return null;
    }
}