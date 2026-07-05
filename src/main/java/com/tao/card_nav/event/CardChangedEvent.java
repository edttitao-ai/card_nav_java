package com.tao.card_nav.event;

import java.time.Instant;

/**
 * 卡片变更事件（Spring 应用事件）。
 *
 * <p>发布者：CardsService 在 addCard/updateCard/deleteCard/togglePinned 成功后发布。
 * <p>监听者：例如 CardLogEventListener，负责写操作日志；后续可加"AI 索引刷新""统计重算"等监听者。
 *
 * <p>设计要点：
 * <ul>
 *   <li>事件本身不带 operatorIp，由监听者按需自行解析（同步线程能从 RequestContextHolder 取，异步/响应式线程 fallback）。</li>
 *   <li>操作类型用枚举约束，避免原来 AOP 通过方法名 startsWith 推断的脆弱逻辑。</li>
 * </ul>
 */
public record CardChangedEvent(Long cardId, CardAction action, Instant occurredAt) {

    public CardChangedEvent(Long cardId, CardAction action) {
        this(cardId, action, Instant.now());
    }

    /**
     * 卡片变更操作类型。写入 card_logs.action 字段时直接 .name()。
     */
    public enum CardAction {
        INSERT,
        UPDATE,
        DELETE,
        TOGGLE_PINNED
    }
}