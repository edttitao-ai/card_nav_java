-- V1: 卡片删除申请历史表（card_delete_history）
--
-- 用途：审计 + 历史查询。S-1 重构后，AI 删除卡片走"申请验证 → 用户确认"两步走，
--       本表记录每一步的申请与结果。
--
-- 状态机：
--   REQUESTED：AI 调用「申请删除验证」后写入
--   APPROVED ：AI 调用「确认删除卡片」并校验通过后写入
--   REJECTED ：当前阶段未触发，留待阶段 2（人工审核）启用
--
-- 与 Redis 中"一次性验证码"的区别：
--   Redis 存的是 5 分钟过期的验证码；本表存的是永久审计记录。

CREATE TABLE IF NOT EXISTS card_delete_history (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  card_id      BIGINT NOT NULL                         COMMENT '被申请删除的卡片 id',
  card_title   VARCHAR(255)                            COMMENT '申请时刻的卡片标题快照',
  status       VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED / APPROVED / REJECTED',
  reason       VARCHAR(500)                            COMMENT '申请理由（AI 描述或人工备注）',
  operator_ip  VARCHAR(64)                             COMMENT '申请时的 IP（来自 ClientIpUtils.resolveCurrent）',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP    COMMENT '申请时间',
  INDEX idx_card_id    (card_id),
  INDEX idx_created_at (created_at),
  INDEX idx_status     (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='卡片删除申请历史（S-1 安全重构引入）';