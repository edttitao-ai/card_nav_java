package com.tao.card_nav.event;

import com.tao.card_nav.service.CardLogsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 卡片操作日志监听器。
 *
 * <p>替代原 CardLogAspect：原 AOP 在响应式 / 异步线程中调
 * {@code RequestContextHolder.getRequestAttributes()} 会抛
 * "No thread-bound request found"，导致 AI 工具调用时日志根本没写。
 * <p>本监听器在 try/catch 内解析 IP，拿不到 request 时 fallback 为 "ai"，
 * 保证响应式路径也能稳定写日志。
 */
@Component
@RequiredArgsConstructor
public class CardLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(CardLogEventListener.class);

    /** 当事件发生在非 Web 请求线程（Reactor / 异步 / 定时任务）时使用的占位符 */
    private static final String IP_FALLBACK = "ai";

    private final CardLogsService cardLogsService;

    @EventListener
    public void onCardChanged(CardChangedEvent event) {
        String operatorIp = resolveOperatorIp();
        try {
            cardLogsService.logAction(event.cardId(), event.action().name(), operatorIp);
        } catch (Exception e) {
            // 日志写库失败不影响主业务，但必须用 logger 留痕
            log.error("记录卡片操作日志失败: cardId={}, action={}", event.cardId(), event.action(), e);
        }
    }

    /**
     * 解析当前请求 IP。
     * <ul>
     *   <li>同步 Web 请求线程：从 RequestContextHolder 取，按 X-Forwarded-For / Proxy-Client-IP / WL-Proxy-Client-IP / remoteAddr 顺序兜底。</li>
     *   <li>其他线程（Reactor / 异步）：拿不到 request，fallback "ai"。</li>
     * </ul>
     */
    private String resolveOperatorIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return IP_FALLBACK;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (isInvalid(ip)) ip = request.getHeader("Proxy-Client-IP");
            if (isInvalid(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
            if (isInvalid(ip)) ip = request.getRemoteAddr();
            return ip == null ? IP_FALLBACK : ip;
        } catch (Exception e) {
            return IP_FALLBACK;
        }
    }

    private boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}