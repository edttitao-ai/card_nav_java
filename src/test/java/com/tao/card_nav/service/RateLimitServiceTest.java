package com.tao.card_nav.service;

import com.tao.card_nav.exception.RateLimitException;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RateLimitService 单元测试：使用真实 RateLimiterRegistry 实例验证按 key 隔离的令牌桶行为。
 *
 * <p>每个测试使用独立的 RateLimiterRegistry（不共享状态），通过 RateLimiterConfig 控制时间窗口，
 * 避免依赖系统时钟和单测运行速度。
 */
class RateLimitServiceTest {

    /** 极短窗口 + 极小配额，便于测试 */
    private RateLimiterRegistry registry;
    private RateLimitService service;

    @BeforeEach
    void setUp() {
        // 注意：这里手动构造一个固定配置的 Registry 覆盖 yml 默认值，
        // 测试只用 aiChatByIp 实例名。
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(Duration.ZERO)
                .build();
        registry = RateLimiterRegistry.of(config);
        service = new RateLimitService(registry, 10000);
    }

    @Test
    void acquireOrThrow_allowsUpToLimit_thenThrows() {
        // 前 3 次通过
        service.acquireOrThrow("1.1.1.1");
        service.acquireOrThrow("1.1.1.1");
        service.acquireOrThrow("1.1.1.1");

        // 第 4 次触发限流
        assertThatThrownBy(() -> service.acquireOrThrow("1.1.1.1"))
                .isInstanceOf(RateLimitException.class)
                .extracting("clientIp").isEqualTo("1.1.1.1");
    }

    @Test
    void acquireOrThrow_differentKeys_isolated() {
        // IP A 用完配额
        service.acquireOrThrow("1.1.1.1");
        service.acquireOrThrow("1.1.1.1");
        service.acquireOrThrow("1.1.1.1");
        assertThatThrownBy(() -> service.acquireOrThrow("1.1.1.1"))
                .isInstanceOf(RateLimitException.class);

        // IP B 不受影响
        service.acquireOrThrow("2.2.2.2");
        service.acquireOrThrow("2.2.2.2");
        service.acquireOrThrow("2.2.2.2");
        assertThatThrownBy(() -> service.acquireOrThrow("2.2.2.2"))
                .isInstanceOf(RateLimitException.class);
    }

    @Test
    void acquireOrThrow_nullKey_normalizedToUnknown() {
        service.acquireOrThrow(null);
        service.acquireOrThrow("");
        service.acquireOrThrow(null);
        // 第 4 次：null 也被规整为 "unknown"，共享同一 bucket
        assertThatThrownBy(() -> service.acquireOrThrow(null))
                .isInstanceOf(RateLimitException.class)
                .extracting("clientIp").isEqualTo("unknown");
    }

    @Test
    void rateLimitException_carriesRetryAfter() {
        try {
            // 耗尽配额
            service.acquireOrThrow("9.9.9.9");
            service.acquireOrThrow("9.9.9.9");
            service.acquireOrThrow("9.9.9.9");
        } catch (Exception ignored) {
            // 不应到这里
        }

        try {
            service.acquireOrThrow("9.9.9.9");
        } catch (RateLimitException ex) {
            assertThat(ex.getClientIp()).isEqualTo("9.9.9.9");
            assertThat(ex.getRetryAfterSeconds()).isEqualTo(60L);
            return;
        }
        org.junit.jupiter.api.Assertions.fail("应该抛出 RateLimitException");
    }
}