package com.tao.card_nav.service;

import com.tao.card_nav.entity.CardDeleteHistoryDo;
import com.tao.card_nav.mapper.CardDeleteHistoryDoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * card_delete_history 业务封装：纯记录写入与查询。
 *
 * <p>本服务只承担"历史查询"职责；卡片是否真删由 {@code CardsService} 控制。
 * 写历史记录用 try/catch 包裹，单条写失败不应阻断主业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardDeleteHistoryService {

    private final CardDeleteHistoryDoMapper mapper;

    /**
     * 记录一次删除申请。
     *
     * @param cardId    卡片 id
     * @param cardTitle 卡片标题快照（卡片可能随后被软删除）
     * @param reason    申请理由（可空）
     * @param operatorIp 操作者 IP（可空，匿名场景）
     * @param status    {@link CardDeleteHistoryDo.Status#REQUESTED} 等
     */
    public void recordRequest(Long cardId, String cardTitle, String reason,
                              String operatorIp, String status) {
        try {
            CardDeleteHistoryDo rec = CardDeleteHistoryDo.builder()
                    .cardId(cardId)
                    .cardTitle(cardTitle)
                    .reason(reason)
                    .operatorIp(operatorIp)
                    .status(status != null ? status : CardDeleteHistoryDo.Status.REQUESTED)
                    .createdAt(new Date())
                    .build();
            mapper.insertSelective(rec);
            log.debug("CardDeleteHistory 写入成功: cardId={}, status={}", cardId, rec.getStatus());
        } catch (Exception e) {
            // 写历史失败不应阻断主业务（删除流程已成功）
            log.error("CardDeleteHistory 写入失败: cardId={}", cardId, e);
        }
    }

    /**
     * 查询最近 limit 条历史。limit 默认 50。
     */
    public List<CardDeleteHistoryDo> listRecent(Integer limit) {
        int n = (limit == null || limit <= 0) ? 50 : limit;
        return mapper.selectAllOrderByCreatedAtDesc(n);
    }
}