package com.tao.card_nav.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache-Aside 配置：Spring Cache + Caffeine（本地内存）
 *
 * <p>适用场景：读多写少的准静态数据（侧边栏 / 分类）。
 * <p>策略：写时失效（@CacheEvict）+ TTL 10 分钟兜底，防止 evict 遗漏导致的长期脏数据。
 * <p>新增缓存名时，往 {@link #CACHE_NAMES} 里追加即可。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 所有缓存名集中在此，便于审计与扩展 */
    public static final String CACHE_SIDEBARS = "sidebars";
    public static final String CACHE_CATEGORIES = "categories";

    private static final String[] CACHE_NAMES = {CACHE_SIDEBARS, CACHE_CATEGORIES};

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(CACHE_NAMES);
        mgr.setCaffeine(Caffeine.newBuilder()
                // 写后 10 分钟过期：与人工误操作兜底窗口对齐
                .expireAfterWrite(10, TimeUnit.MINUTES)
                // 单实例条目上限：侧边栏/分类是几十条级别，留出余量
                .maximumSize(500)
                // 统计命中率（调试 / 监控用）
                .recordStats());
        return mgr;
    }
}