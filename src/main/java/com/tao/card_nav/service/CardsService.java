package com.tao.card_nav.service;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.event.CardChangedEvent;
import com.tao.card_nav.event.CardChangedEvent.CardAction;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.mapper.CardsDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardsService {

    private final CardsDoMapper cardsMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 查询所有卡片，支持按 sidebarId 过滤
     */
    public List<CardsDo> getCards(String sidebarId) {
        if (sidebarId != null && !"".equals(sidebarId)) {
            return cardsMapper.selectBySidebarId(sidebarId);
        }
        // 查询所有未删除的卡片
        return cardsMapper.selectAll();
    }

    /**
     * AI 场景用：按侧边栏查询，硬上限 limit 条（pinned 优先 + id 倒序）
     */
    public List<CardsDo> getCardsLimited(String sidebarId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (sidebarId != null && !sidebarId.isEmpty()) {
            return cardsMapper.selectBySidebarIdLimited(sidebarId, limit);
        }
        return cardsMapper.selectAllLimited(limit);
    }

    /**
     * AI 场景用：按 keyword + sidebarId + categoryId 检索，硬上限 limit 条
     * keyword 匹配 title/description/url；其他条件为空则不参与过滤
     */
    public List<CardsDo> searchCards(String keyword, String sidebarId, Long categoryId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        return cardsMapper.searchCards(keyword, sidebarId, categoryId, limit);
    }

    /**
     * 按 ID 查询卡片
     */
    public CardsDo getCardById(Long id) {
        CardsDo card = cardsMapper.selectByPrimaryKey(id);
        if (card == null) {
            throw new BusinessException("卡片不存在");
        }
        return card;
    }

    /**
     * 新增卡片
     */
    public CardsDo addCard(CardsDo card) {
        // 查重：先按 URL，再按 title，避免重复创建
        if (card.getUrl() != null && !card.getUrl().isEmpty()) {
            CardsDo existingByUrl = cardsMapper.selectByUrl(card.getUrl());
            if (existingByUrl != null) {
                throw new BusinessException(400, "该链接已存在卡片「" + existingByUrl.getTitle()
                        + "」(id=" + existingByUrl.getId() + ")，无需重复添加");
            }
        }
        if (card.getTitle() != null && !card.getTitle().isEmpty()) {
            CardsDo existingByTitle = cardsMapper.selectByTitle(card.getTitle());
            if (existingByTitle != null) {
                throw new BusinessException(400, "已存在同标题的卡片「" + existingByTitle.getTitle()
                        + "」(id=" + existingByTitle.getId() + ")，请更换标题");
            }
        }

        card.setCreatedAt(new Date());
        card.setUpdatedAt(new Date());
        card.setDeletedAt(null);
        // 默认 pinned 为 false
        if (card.getPinned() == null) {
            card.setPinned(false);
        }
        // 自动生成 externalId（如果为空）
        if (card.getExternalId() == null || card.getExternalId().isEmpty()) {
            card.setExternalId(generateExternalId());
        }
        cardsMapper.insertSelective(card);
        publish(card.getId(), CardAction.INSERT);
        return card;
    }

    /**
     * 生成唯一 externalId
     */
    private String generateExternalId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 发布卡片变更事件（用 cardId = 当前方法操作的 id；调用方应保证 id 非空）
     */
    private void publish(Long cardId, CardAction action) {
        if (cardId == null) {
            return;
        }
        eventPublisher.publishEvent(new CardChangedEvent(cardId, action));
    }

    /**
     * 更新卡片
     */
    public CardsDo updateCard(Long id, CardsDo card) {
        CardsDo existing = cardsMapper.selectByPrimaryKey(id);
        if (existing == null) {
            throw new BusinessException("卡片不存在");
        }

        // 如果改了 URL，校验新 URL 不能与别的卡片冲突
        if (card.getUrl() != null && !card.getUrl().isEmpty() && !card.getUrl().equals(existing.getUrl())) {
            CardsDo conflictByUrl = cardsMapper.selectByUrl(card.getUrl());
            if (conflictByUrl != null && !conflictByUrl.getId().equals(id)) {
                throw new BusinessException(400, "新链接已被其他卡片「" + conflictByUrl.getTitle()
                        + "」(id=" + conflictByUrl.getId() + ")占用");
            }
        }
        // 如果改了 title，校验新 title 不能与别的卡片冲突
        if (card.getTitle() != null && !card.getTitle().isEmpty() && !card.getTitle().equals(existing.getTitle())) {
            CardsDo conflictByTitle = cardsMapper.selectByTitle(card.getTitle());
            if (conflictByTitle != null && !conflictByTitle.getId().equals(id)) {
                throw new BusinessException(400, "新标题已被其他卡片(id=" + conflictByTitle.getId() + ")占用，请更换");
            }
        }

        card.setId(id);
        // 保留原 createdAt，不允许通过更新接口改创建时间
        card.setCreatedAt(existing.getCreatedAt());
        card.setUpdatedAt(new Date());
        // 不允许通过更新接口删除卡片
        card.setDeletedAt(null);
        cardsMapper.updateByPrimaryKeySelective(card);
        CardsDo saved = cardsMapper.selectByPrimaryKey(id);
        publish(id, CardAction.UPDATE);
        return saved;
    }

    /**
     * 删除卡片（软删除）
     */
    public void deleteCard(Long id) {
        CardsDo card = cardsMapper.selectByPrimaryKey(id);
        if (card == null) {
            throw new BusinessException("卡片不存在");
        }
        cardsMapper.softDelete(id);
        publish(id, CardAction.DELETE);
    }

    /**
     * 切换置顶状态
     */
    public CardsDo togglePinned(Long id, Boolean pinned) {
        CardsDo card = cardsMapper.selectByPrimaryKey(id);
        if (card == null) {
            throw new BusinessException("卡片不存在");
        }
        card.setPinned(pinned);
        card.setUpdatedAt(new Date());
        cardsMapper.updateByPrimaryKeySelective(card);
        publish(id, CardAction.TOGGLE_PINNED);
        return card;
    }
}
