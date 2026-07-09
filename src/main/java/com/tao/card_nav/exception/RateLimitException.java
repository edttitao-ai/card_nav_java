package com.tao.card_nav.exception;

import lombok.Getter;

/**
 * 限流异常：携带客户端 IP 与建议重试等待秒数，供上层转换为 429 响应。
 *
 * <p>当前仅在 SSE 流的"流内限流事件"分支使用；controller 层不会直接抛给客户端。
 */
@Getter
public class RateLimitException extends RuntimeException {

    private final String clientIp;
    private final long retryAfterSeconds;

    public RateLimitException(String clientIp, long retryAfterSeconds) {
        super("限流触发 clientIp=" + clientIp);
        this.clientIp = clientIp;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}