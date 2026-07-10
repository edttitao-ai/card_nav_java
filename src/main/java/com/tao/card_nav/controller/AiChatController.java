package com.tao.card_nav.controller;


import cn.hutool.json.JSONUtil;
import com.tao.card_nav.ai.aiService.AiServiceAssistant;
import com.tao.card_nav.ai.aiService.ChatSessionRegistry;
import com.tao.card_nav.annotation.RateLimitKey;
import com.tao.card_nav.annotation.RateLimitedByIp;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.exception.RateLimitException;
import com.tao.card_nav.exception.ThrowUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    @Resource
    private AiServiceAssistant aiService;

    @Resource
    private ChatSessionRegistry sessionRegistry;

    /**
     * AI 流式聊天接口。
     *
     * <p>限流由 {@link RateLimitedByIp} 切面按 IP 拦截；触发时走 {@link #rateLimitFallback}。
     * 客户端 IP 通过 {@link RateLimitKey} 注入，业务方法无需自己解析。
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimitedByIp(fallbackMethod = "rateLimitFallback")
    public Flux<ServerSentEvent<String>> chat(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request,
            @RateLimitKey String clientIp) {
        String userMessage = payload.get("userMessage");
        String sessionId = payload.get("sessionId");
        ThrowUtils.throwIf(userMessage == null || userMessage.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "anon-" + UUID.randomUUID().toString();
        }
        final String finalSessionId = sessionId;

        // 用 Subscription 作为可中断令牌：
        // 前端调 /api/chat/stop 时，我们 cancel 这个 Subscription，
        // Reactor 会立刻停止向下游推送，客户端 fetch 也能感受到中断。
        // 注意：底层 LangChain4j 的 StreamingHandle.cancel() 由于
        // langchain4j-reactor 1.17.x 的适配器限制（issue #5275），
        // 可能无法直接抓取 handle。功能上对用户足够。
        return aiService.chatNav(finalSessionId, userMessage)
                .doOnSubscribe(subscription -> {
                    // 把 subscription 注册到 registry，stop 时能 cancel
                    sessionRegistry.registerSubscription(finalSessionId, subscription);
                })
                .doOnCancel(() -> {
                    sessionRegistry.unregisterSubscription(finalSessionId);
                })
                .doOnComplete(() -> {
                    sessionRegistry.unregisterSubscription(finalSessionId);
                })
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 限流触发时的降级方法。
     *
     * <p>签名必须与 {@link #chat} 一致，末尾追加一个 {@link RateLimitException} 参数。
     * 由 {@code RateLimitedByIpAspect} 通过反射调用。
     */
    @SuppressWarnings("unused") // 通过反射被 AOP 调用
    private Flux<ServerSentEvent<String>> rateLimitFallback(
            Map<String, String> payload,
            HttpServletRequest request,
            String clientIp,
            RateLimitException ex) {
        log.info("AI 聊天请求被限流 clientIp={}", clientIp);
        ServerSentEvent<String> rateLimitedEvent = ServerSentEvent.<String>builder()
                .event("rate_limited")
                .data(JSONUtil.toJsonStr(Map.of(
                        "clientIp", clientIp,
                        "retryAfterSeconds", ex.getRetryAfterSeconds(),
                        "message", "请求过于频繁，请稍后再试"
                )))
                .build();
        ServerSentEvent<String> doneEvent = ServerSentEvent.<String>builder()
                .event("done")
                .data("")
                .build();
        return Flux.concat(Mono.just(rateLimitedEvent), Mono.just(doneEvent));
    }

    /**
     * 中断指定 sessionId 的 AI 流式会话。
     * 返回 200 + success=true 表示有会话被中断；success=false 表示无活跃会话。
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        ThrowUtils.throwIf(sessionId == null || sessionId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "sessionId 不能为空");
        boolean stopped = sessionRegistry.stop(sessionId);
        return ResponseEntity.ok(Map.of(
                "success", stopped,
                "message", stopped ? "已中断" : "该 sessionId 当前没有活跃会话"
        ));
    }
}
