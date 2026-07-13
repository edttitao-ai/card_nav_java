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

    // ---------- issue ----------

    @Test
    void issue_generatesSixDigitCode() {
        DeleteChallengeService.Challenge ch = service.issue(42L);

        assertThat(ch.cardId()).isEqualTo(42L);
        assertThat(ch.code()).hasSize(6);
        assertThat(ch.code()).matches("\\d{6}");
        assertThat(ch.expiresIn()).isEqualTo(300L); // 5 分钟
        verify(valueOps).set(eq("del:42:" + ch.code()), eq("1"), any());
    }

    @Test
    void issue_nullCardId_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.issue(null));
    }

    @Test
    void issue_generatesDifferentCodesAcrossCalls() {
        // 由于随机生成，两次调用几乎不可能相同
        String c1 = service.issue(1L).code();
        String c2 = service.issue(1L).code();
        assertThat(c1).isNotEqualTo(c2);
    }

    // ---------- consume ----------

    @Test
    void consume_existingKey_returnsTrue() {
        when(redis.delete("del:1:123456")).thenReturn(true);

        assertThat(service.consume(1L, "123456")).isTrue();
        verify(redis, times(1)).delete("del:1:123456");
    }

    @Test
    void consume_nonExistingKey_returnsFalse() {
        when(redis.delete(anyString())).thenReturn(false);

        assertThat(service.consume(1L, "123456")).isFalse();
    }

    @Test
    void consume_alreadyConsumedKey_returnsFalse() {
        // 第二次取时已不存在
        when(redis.delete("del:1:123456")).thenReturn(false);

        assertThat(service.consume(1L, "123456")).isFalse();
    }

    @Test
    void consume_nullCardId_returnsFalse() {
        assertThat(service.consume(null, "123456")).isFalse();
        verify(redis, times(0)).delete(anyString());
    }

    @Test
    void consume_nullCode_returnsFalse() {
        assertThat(service.consume(1L, null)).isFalse();
        verify(redis, times(0)).delete(anyString());
    }

    @Test
    void consume_invalidCodeLength_returnsFalse() {
        assertThat(service.consume(1L, "12345")).isFalse();    // 5 位
        assertThat(service.consume(1L, "1234567")).isFalse();  // 7 位
        assertThat(service.consume(1L, "")).isFalse();
        verify(redis, times(0)).delete(anyString());
    }

    @Test
    void consume_invalidCodeContent_returnsFalse() {
        // 必须是数字
        assertThat(service.consume(1L, "abcdef")).isFalse();
        assertThat(service.consume(1L, "12345a")).isFalse();
        verify(redis, times(0)).delete(anyString());
    }
}