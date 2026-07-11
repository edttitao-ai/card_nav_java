package com.tao.card_nav.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpUtilsTest {

    @AfterEach
    void tearDown() {
        // 避免测试间 RequestContextHolder 状态污染
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolve_prefersXForwardedFor_firstHop() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1, 10.0.0.2");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("203.0.113.1");
    }

    @Test
    void resolve_fallsBackToXRealIp() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn("198.51.100.5");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("198.51.100.5");
    }

    @Test
    void resolve_fallsBackToRemoteAddr() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("192.0.2.42");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("192.0.2.42");
    }

    @Test
    void resolve_unknownHeaderTreatedAsMissing() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(req.getHeader("X-Real-IP")).thenReturn("unknown");
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("10.0.0.1");
    }

    @Test
    void resolve_ipv6LocalhostNormalizedToIpv4() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("127.0.0.1");
    }

    @Test
    void resolve_nullRequestReturnsUnknown() {
        assertThat(ClientIpUtils.resolve(null)).isEqualTo("unknown");
    }

    @Test
    void resolve_trimsWhitespace() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("  198.51.100.7  , 10.0.0.1");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpUtils.resolve(req)).isEqualTo("198.51.100.7");
    }

    @Test
    void resolveCurrent_returnsNullWhenNoRequestBound() {
        // 未调用 setRequestAttributes，RequestContextHolder 返回 null
        assertThat(ClientIpUtils.resolveCurrent()).isNull();
    }

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