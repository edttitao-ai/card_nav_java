package com.tao.card_nav.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ClientIpUtils} 单测。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@link ClientIpUtils#resolve(HttpServletRequest)}：反代 IP 解析优先级</li>
 *   <li>{@link ClientIpUtils#resolveCurrent()}：从 RequestContextHolder 取当前请求</li>
 * </ul>
 */
class ClientIpUtilsTest {

    /**
     * 清理 RequestContextHolder，防止测试间 ThreadLocal 污染
     * （如果上一个测试 setRequestAttributes 而下一个测试未清，就会带过来）。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ============================================================
    // ClientIpUtils.resolve(HttpServletRequest)
    // ============================================================

    /**
     * X-Forwarded-For 是多 IP 链 "client, proxy1, proxy2"：必须取第一段。
     * 这是反代场景下识别真实客户端的唯一可靠字段。
     */
    @Test
    void resolve_prefersXForwardedFor_firstHop() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1, 10.0.0.2");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("203.0.113.1");
    }

    /**
     * 没设 X-Forwarded-For：回退到 X-Real-IP（Nginx 默认）。
     */
    @Test
    void resolve_fallsBackToXRealIp() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn("198.51.100.5");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("198.51.100.5");
    }

    /**
     * X-Forwarded-For 与 X-Real-IP 都没有：回退到 remoteAddr（直连场景）。
     */
    @Test
    void resolve_fallsBackToRemoteAddr() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("192.0.2.42");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("192.0.2.42");
    }

    /**
     * 反代头被恶意/异常设为 "unknown" 字面量：视为缺失，回退到下一层。
     * 防止某些反代在不知道真实 IP 时显式写入 "unknown" 占位。
     */
    @Test
    void resolve_unknownHeaderTreatedAsMissing() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(req.getHeader("X-Real-IP")).thenReturn("unknown");
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("10.0.0.1");
    }

    /**
     * IPv6 本机回环 "0:0:0:0:0:0:0:1" 归一化为 IPv4 "127.0.0.1"：
     * 本地调试时少踩 IPv4/IPv6 不一致导致的限流 key 漂移。
     */
    @Test
    void resolve_ipv6LocalhostNormalizedToIpv4() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("127.0.0.1");
    }

    /**
     * request 为 null：返回 "unknown" 哨兵字符串，
     * 而不是抛 NPE 或返回 null（避免调用方到处判 null）。
     */
    @Test
    void resolve_nullRequestReturnsUnknown() {
        assertThat(ClientIpUtils.resolve(null)).isEqualTo("unknown");
    }

    /**
     * X-Forwarded-For 各段可能有前后空格：trim 后再判定。
     */
    @Test
    void resolve_trimsWhitespace() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("  198.51.100.7  , 10.0.0.1");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("198.51.100.7");
    }

    // ============================================================
    // ClientIpUtils.resolveCurrent()
    // ============================================================

    /**
     * 当前线程没有绑定的 request（异步 / 事件监听器场景）：
     * resolveCurrent 返回 null 而不抛 NPE，由调用方决定 fallback。
     */
    @Test
    void resolveCurrent_returnsNullWhenNoRequestBound() {
        // tearDown 已保证 RequestContextHolder 清空；这里直接验证
        assertThat(ClientIpUtils.resolveCurrent()).isNull();
    }

    /**
     * 当前线程已通过 ServletRequestAttributes 绑定 request：
     * resolveCurrent 取出并复用 resolve 的解析逻辑。
     */
    @Test
    void resolveCurrent_resolvesFromCurrentRequest() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.99, 10.0.0.1");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        org.springframework.web.context.request.ServletRequestAttributes attrs =
                new org.springframework.web.context.request.ServletRequestAttributes(req);
        RequestContextHolder.setRequestAttributes(attrs);

        assertThat(ClientIpUtils.resolveCurrent()).isEqualTo("203.0.113.99");
    }
}