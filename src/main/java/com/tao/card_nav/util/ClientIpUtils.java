package com.tao.card_nav.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端真实 IP 解析工具。
 *
 * <p>优先级：
 * <ol>
 *   <li>{@code X-Forwarded-For} 第一段（最常见反代头）</li>
 *   <li>{@code X-Real-IP}（Nginx 默认）</li>
 *   <li>{@code request.getRemoteAddr()} 兜底</li>
 * </ol>
 *
 * <p>注意：依赖反代正确写入转发头；如果是直接暴露在公网，应去掉信任头这一层。
 */
public final class ClientIpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";

    private ClientIpUtils() {}

    /**
     * 解析客户端真实 IP。
     *
     * @param request 当前 HTTP 请求
     * @return 客户端 IP；解析失败时返回 {@code "unknown"}
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = extractFromHeader(request, "X-Forwarded-For");
        if (isValid(ip)) {
            // X-Forwarded-For 可能是 "client, proxy1, proxy2"，取第一段
            int comma = ip.indexOf(',');
            ip = (comma > -1 ? ip.substring(0, comma) : ip).trim();
            if (isValid(ip)) {
                return normalize(ip);
            }
        }

        ip = extractFromHeader(request, "X-Real-IP");
        if (isValid(ip)) {
            return normalize(ip);
        }

        return normalize(request.getRemoteAddr());
    }

    private static String extractFromHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? null : value.trim();
    }

    private static boolean isValid(String ip) {
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        return true;
    }

    private static String normalize(String ip) {
        if (ip == null || ip.isEmpty()) {
            return UNKNOWN;
        }
        // IPv6 本机回环地址归一为 IPv4
        if (LOCALHOST_IPV6.equals(ip)) {
            return LOCALHOST_IPV4;
        }
        return ip;
    }
}