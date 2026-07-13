package com.tao.card_nav.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 卡片删除申请历史（纯记录表）。
 *
 * <p>用途：审计 + 历史查询。
 * 与 Redis 中的"验证码"解耦——Redis 仅用于一次性 5 分钟验证，本表用于事后追溯。
 *
 * <p>字段：
 * <ul>
 *   <li>{@code cardId}：被申请删除的卡片 id</li>
 *   <li>{@code cardTitle}：申请时刻的卡片标题快照（卡片删除后仍能展示当时是谁）</li>
 *   <li>{@code status}：REQUESTED / APPROVED / REJECTED</li>
 *   <li>{@code reason}：AI 描述或人工备注</li>
 *   <li>{@code operatorIp}：申请时的 IP（来自 ClientIpUtils.resolveCurrent）</li>
 *   <li>{@code createdAt}：申请时间</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardDeleteHistoryDo {
    private Long id;
    private Long cardId;
    private String cardTitle;
    private String status;
    private String reason;
    private String operatorIp;
    private Date createdAt;

    /** 状态枚举值（与 DB 字段对应，避免拼写错误） */
    public static final class Status {
        public static final String REQUESTED = "REQUESTED";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        private Status() {}
    }
}