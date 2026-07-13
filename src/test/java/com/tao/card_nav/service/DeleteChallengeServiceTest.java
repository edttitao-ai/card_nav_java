package com.tao.card_nav.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DeleteChallengeService} 单测（mock {@link StringRedisTemplate}）。
 *
 * <p>测的核心契约：
 * <ul>
 *   <li>issue：生成 6 位数字 code，写入 Redis key "del:{cardId}:{code}"，TTL 5 分钟</li>
 *   <li>consume：必须先校验 6 位数字格式；存在则 redis.delete 是天然的"一次性"消费</li>
 *   <li>参数非法（null cardId / null code / 长度不对 / 含字母）一律返回 false，不调 redis</li>
 * </ul>
 */
class DeleteChallengeServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private DeleteChallengeService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new DeleteChallengeService(redis);
    }

    // ============================================================
    // issue
    // ============================================================

    /**
     * 正常路径：返回的 Challenge 包含 cardId / 6 位数字 code / 300 秒 TTL（5 分钟）。
     * 并验证 Redis 写入了正确格式的 key "del:{cardId}:{code}"。
     */
    @Test
    void issue_generatesSixDigitCode() {
        DeleteChallengeService.Challenge ch = service.issue(42L);

        assertThat(ch.cardId()).isEqualTo(42L);
        assertThat(ch.code()).hasSize(6);
        assertThat(ch.code()).matches("\\d{6}");
        assertThat(ch.expiresIn()).isEqualTo(300L); // 5 分钟
        verify(valueOps).set(eq("del:42:" + ch.code()), eq("1"), any());
    }

    /**
     * 防御性：cardId 为 null 必须抛 IllegalArgumentException，不允许写入半截 key。
     */
    @Test
    void issue_nullCardId_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.issue(null));
    }

    /**
     * <b>随机性保护</b>：连续两次 issue 不应该生成同一个 code
     * （即使 cardId 相同）。保证攻击者无法预测。
     */
    @Test
    void issue_generatesDifferentCodesAcrossCalls() {
        String c1 = service.issue(1L).code();
        String c2 = service.issue(1L).code();
        assertThat(c1).isNotEqualTo(c2);
    }

    // ============================================================
    // consume
    // ============================================================

    /**
     * 正常路径：key 存在 → redis.delete 返回 true → consume 返回 true，
     * 同时 Redis 自动失效（一次性消费）。
     */
    @Test
    void consume_existingKey_returnsTrue() {
        when(redis.delete("del:1:123456")).thenReturn(true);

        assertThat(service.consume(1L, "123456")).isTrue();
        verify(redis, times(1)).delete("del:1:123456");
    }

    /**
     * key 不存在（过期或被消费过）：redis.delete 返回 false → consume 返回 false。
     */
    @Test
    void consume_nonExistingKey_returnsFalse() {
        when(redis.delete(anyString())).thenReturn(false);

        assertThat(service.consume(1L, "123456")).isFalse();
    }

    /**
     * 已消费的 key：再次 consume 应返 false（不可复用）。
     * 这是"一次性"语义的回归保护。
     */
    @Test
    void consume_alreadyConsumedKey_returnsFalse() {
        when(redis.delete("del:1:123456")).thenReturn(false);

        assertThat(service.consume(1L, "123456")).isFalse();
    }

    /**
     * cardId 为 null：直接返 false，不调 Redis。
     * 防止 "del:null:123456" 这种半成品 key 污染 Redis。
     */
    @Test
    void consume_nullCardId_returnsFalse() {
        assertThat(service.consume(null, "123456")).isFalse();
        verify(redis, times(0)).delete(anyString());
    }

    /**
     * code 为 null：直接返 false，不调 Redis。
     */
    @Test
    void consume_nullCode_returnsFalse() {
        assertThat(service.consume(1L, null)).isFalse();
        verify(redis, times(0)).delete(anyString());
    }

    /**
     * 长度非法：5 位 / 7 位 / 空串均不通过。
     * 防止 Redis 被 "del:1:1" 这种短码污染。
     */
    @Test
    void consume_invalidCodeLength_returnsFalse() {
        assertThat(service.consume(1L, "12345")).isFalse();    // 5 位
        assertThat(service.consume(1L, "1234567")).isFalse();  // 7 位
        assertThat(service.consume(1L, "")).isFalse();
        verify(redis, times(0)).delete(anyString());
    }

    /**
     * 内容非法：必须是 6 位数字；含字母或混合直接返 false。
     * 防止 LLM "创造" 看似合法的非数字 code。
     */
    @Test
    void consume_invalidCodeContent_returnsFalse() {
        assertThat(service.consume(1L, "abcdef")).isFalse();
        assertThat(service.consume(1L, "12345a")).isFalse();
        verify(redis, times(0)).delete(anyString());
    }
}