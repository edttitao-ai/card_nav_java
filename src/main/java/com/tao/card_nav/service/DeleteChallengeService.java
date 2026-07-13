package com.tao.card_nav.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 删除卡片的一次性验证码机制（Redis 实现）。
 *
 * <p><b>为什么用 Redis</b>：
 * <ul>
 *   <li>TTL 5 分钟自动过期，无需手写定时清理；</li>
 *   <li>{@code redis.delete(key)} 是天然"一次性消费"——读 + 删原子完成；</li>
 *   <li>无状态横向扩展。</li>
 * </ul>
 *
 * <p><b>语义</b>：
 * <ul>
 *   <li>{@link #issue(Long)} 生成 6 位数字 code，绑定 cardId，5 分钟 TTL；</li>
 *   <li>{@link #consume(Long, String)} 校验 code 格式（必须 6 位数字）+ 存在性，成功立即失效；</li>
 *   <li>code 是"使用即失效"语义，无法复用；</li>
 *   <li>code 仅校验 6 位数字精确匹配，不做模糊。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteChallengeService {

    /** Redis key 模板：del:{cardId}:{code} */
    private static final String KEY_TPL = "del:%d:%s";

    /** TTL 5 分钟 */
    private static final Duration TTL = Duration.ofMinutes(5);

    /** 6 位 code */
    private static final int CODE_LEN = 6;

    private final StringRedisTemplate redis;

    /**
     * 申请一次验证码。
     *
     * @return 含 cardId / code / 有效期秒数（开发/调试用，生产仅返回 challengeId）
     */
    public Challenge issue(Long cardId) {
        if (cardId == null) {
            throw new IllegalArgumentException("cardId 不能为空");
        }
        String code = generateCode();
        redis.opsForValue().set(KEY_TPL.formatted(cardId, code), "1", TTL);
        log.debug("DeleteChallenge 已签发: cardId={}, code={}, ttl=5m", cardId, code);
        return new Challenge(cardId, code, TTL.toSeconds());
    }

    /**
     * 校验并消费 code。成功返回 true；失败 / 过期 / 一次性已用 → false。
     *
     * <p>实现：{@code redis.delete(key)} 本身是"读 + 删"原子操作，
     * 返回 true 表示 key 之前存在 → 校验通过且自动失效。
     */
    public boolean consume(Long cardId, String code) {
        if (cardId == null || code == null || !isValidCode(code)) {
            return false;
        }
        String key = KEY_TPL.formatted(cardId, code);
        Boolean deleted = redis.delete(key);
        boolean ok = Boolean.TRUE.equals(deleted);
        log.debug("DeleteChallenge 消费: cardId={}, code={}, ok={}", cardId, code, ok);
        return ok;
    }

    private static boolean isValidCode(String code) {
        if (code.length() != CODE_LEN) return false;
        for (int i = 0; i < CODE_LEN; i++) {
            char c = code.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static String generateCode() {
        // ThreadLocalRandom 比 Math.random 更适合多线程场景
        StringBuilder sb = new StringBuilder(CODE_LEN);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append((char) ('0' + rnd.nextInt(10)));
        }
        return sb.toString();
    }

    /**
     * 签发记录。code 在生产环境不应回传，仅 challengeId / expiresIn 足够。
     * 调试 / 自测环境才回传 code 便于复制。
     */
    public record Challenge(Long cardId, String code, long expiresIn) {}
}