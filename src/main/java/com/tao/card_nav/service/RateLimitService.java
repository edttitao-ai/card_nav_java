package com.tao.card_nav.service;

import com.tao.card_nav.exception.RateLimitException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 限流服务：基于 Resilience4j RateLimiter 按 key 隔离令牌桶。
 *
 * <p>注意：当前实现是 JVM 内存版，多实例部署时单 IP 实际配额 = 配额 × 实例数。
 * 文档里需要明确这一限制；如果后续需要严格跨实例共享，应切换到 Bucket4j + Redis。
 *
 * <p>实现说明：Resilience4j 的 RateLimiterRegistry 在 Spring Boot starter 中，
 * 实例配置来自 application.yml。但 starter 默认不允许"按运行时 key 隔离"——
 * 我们用 ConcurrentHashMap 自己按 key 缓存（每个 key 独立 RateLimiter 实例，
 * 但复用 registry 里 aiChatByIp 的配置）。
 */
@Slf4j
@Service
public class RateLimitService {

    /** 配置在 application.yml 的限流实例名（每 IP 每分钟 5 次） */
    public static final String AI_CHAT_INSTANCE = "aiChatByIp";

    /** 按 key 缓存的 RateLimiter 实例，实现"每 IP 一个独立令牌桶" */
    private final ConcurrentMap<String, RateLimiter> limiterCache = new ConcurrentHashMap<>();

    /** max-allowable-tags 上限：超过这个唯一 key 数就不再新建 limiter（防止恶意构造 IP 撑爆内存） */
    private final int maxAllowableTags;

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimitService(
            RateLimiterRegistry rateLimiterRegistry,
            @Value("${resilience4j.ratelimiter.instances.aiChatByIp.max-allowable-tags:10000}") int maxAllowableTags) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.maxAllowableTags = maxAllowableTags;
    }

    /**
     * 尝试为指定 key 获取 1 个令牌。
     *
     * @param key 业务键（典型用法：客户端 IP）
     * @throws RateLimitException 当配额耗尽时抛出
     */
    public void acquireOrThrow(String key) {
        if (key == null || key.isEmpty()) {
            key = "unknown";
        }

        RateLimiter limiter = limiterCache.computeIfAbsent(key, this::createLimiter);
        if (limiter == null) {
            // 超过 max-allowable-tags 上限，放行避免反伤正常用户
            return;
        }

        if (!limiter.acquirePermission()) {
            RateLimiterConfig config = limiter.getRateLimiterConfig();
            long retryAfter = config.getLimitRefreshPeriod().toSeconds();
            log.info("AI 聊天限流触发 key={} retryAfter={}s", key, retryAfter);
            throw new RateLimitException(key, retryAfter);
        }
    }

    /**
     * 为指定 key 创建一个新的 RateLimiter 实例。
     *
     * <p>复用 registry 里 aiChatByIp 的配置（来自 application.yml）。
     * 每个 key 用独立名字（aiChatByIp:{key}）避免覆盖 starter 创建的单例。
     */
    private RateLimiter createLimiter(String key) {
        if (limiterCache.size() >= maxAllowableTags) {
            log.warn("限流实例数已达上限 max={}，拒绝再为 key={} 创建 limiter", maxAllowableTags, key);
            return null;
        }
        try {
            RateLimiterConfig config = rateLimiterRegistry
                    .rateLimiter(AI_CHAT_INSTANCE)
                    .getRateLimiterConfig();
            return RateLimiter.of(AI_CHAT_INSTANCE + ":" + key, config);
        } catch (Exception e) {
            log.warn("RateLimiter 创建失败 key={}", key, e);
            return null;
        }
    }
}