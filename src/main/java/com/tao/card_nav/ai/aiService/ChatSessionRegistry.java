package com.tao.card_nav.ai.aiService;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存每个正在进行的流式会话对应的 Reactor Subscription，
 * 便于外部（例如 /api/chat/stop 接口）按 sessionId 主动取消流。
 *
 * 说明：
 * 1. langchain4j-reactor 1.17.x 的 Flux<String> 适配器无法直接暴露
 *    底层的 StreamingHandle（参考 langchain4j issue #5275），
 *    所以这里使用 Subscription.cancel() 作为可中断令牌。
 *    客户端体验是：cancel 后 Reactor 立刻停止推送，前端 fetch abort 立即生效。
 * 2. 底层 LLM SSE 连接可能短暂保持，但客户端已收到 abort，体感上"立刻停了"。
 * 3. 同时支持 register/unregister 与 stop，便于运维追踪活跃会话。
 */
@Slf4j
@Component
public class ChatSessionRegistry {

    private final Map<String, Subscription> subscriptionMap = new ConcurrentHashMap<>();

    /**
     * 注册一个新的 Subscription 到指定 sessionId。
     * 如果之前还有未释放的订阅，自动 cancel 旧的（防止泄漏）。
     */
    public void registerSubscription(String sessionId, Subscription subscription) {
        if (sessionId == null || subscription == null) return;
        Subscription prev = subscriptionMap.put(sessionId, subscription);
        if (prev != null) {
            log.warn("sessionId={} 之前还有一个 Subscription 未清理，将自动 cancel", sessionId);
            try { prev.cancel(); } catch (Exception ignored) {}
        }
    }

    /**
     * 流结束或异常时清理。
     */
    public void unregisterSubscription(String sessionId) {
        if (sessionId == null) return;
        subscriptionMap.remove(sessionId);
    }

    /**
     * 主动中断指定 sessionId 的流。
     * @return true 表示有会话被取消；false 表示无活跃会话。
     */
    public boolean stop(String sessionId) {
        Subscription subscription = subscriptionMap.remove(sessionId);
        if (subscription == null) return false;
        try {
            subscription.cancel();
            log.info("已中断 sessionId={} 的 AI 流式会话", sessionId);
            return true;
        } catch (Exception e) {
            log.error("中断 sessionId={} 失败", sessionId, e);
            return false;
        }
    }
}
