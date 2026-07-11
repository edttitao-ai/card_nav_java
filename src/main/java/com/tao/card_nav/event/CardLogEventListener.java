package com.tao.card_nav.event;

import com.tao.card_nav.service.CardLogsService;
import com.tao.card_nav.util.ClientIpUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 卡片操作日志监听器。
 *
 * <p>替代原 CardLogAspect：原 AOP 在响应式 / 异步线程中调
 * {@code RequestContextHolder.getRequestAttributes()} 会抛
 * "No thread-bound request found"，导致 AI 工具调用时日志根本没写。
 *
 * <p>本监听器在取不到 IP 时 fallback 为 {@link #IP_FALLBACK}（业务约定），保证
 * 响应式路径也能稳定写日志。IP 解析统一走 {@link ClientIpUtils#resolveCurrent()}。
 */
@Component
@RequiredArgsConstructor
public class CardLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(CardLogEventListener.class);

    /** 当事件发生在非 Web 请求线程（Reactor / 异步 / 定时任务）时使用的业务占位符 */
    private static final String IP_FALLBACK = "ai";

    private final CardLogsService cardLogsService;

    @EventListener
    public void onCardChanged(CardChangedEvent event) {
        String ip = ClientIpUtils.resolveCurrent();
        // 非 Web 请求线程：使用业务约定的占位符
        if (ip == null) {
            ip = IP_FALLBACK;
        }

        try {
            cardLogsService.logAction(event.cardId(), event.action().name(), ip);
        } catch (Exception e) {
            // 日志写库失败不影响主业务，但必须用 logger 留痕
            log.error("记录卡片操作日志失败: cardId={}, action={}", event.cardId(), event.action(), e);
        }
    }
}
