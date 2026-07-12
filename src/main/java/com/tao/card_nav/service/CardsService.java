package com.tao.card_nav.service;

import com.tao.card_nav.domain.CardPatchNormalizer;
import com.tao.card_nav.domain.CardUniquenessPolicy;
import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.event.CardChangedEvent;
import com.tao.card_nav.event.CardChangedEvent.CardAction;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;
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
    private final CardUniquenessPolicy uniquenessPolicy;

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
    public CardsDo addCard(CardsDo rawCard) {
        // 1. 规范化输入：空串/纯空白 → null，避免选择性 mapper 写入空串
        CardsDo card = CardPatchNormalizer.normalize(rawCard);

        // 2. 唯一性校验（URL → Title 级联）；新增场景下不排除任何卡片
        uniquenessPolicy.validateUniqueness(null, card.getUrl(), card.getTitle());

        // 3. 写入前补默认值
        card.setCreatedAt(new Date());
        card.setUpdatedAt(new Date());
        // 默认 pinned 为 false
        if (card.getPinned() == null) {
            card.setPinned(false);
        }
        // 自动生成 externalId（如果为空）
        if (card.getExternalId() == null) {
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
    public CardsDo updateCard(Long id, CardsDo rawCard) {
        CardsDo existing = cardsMapper.selectByPrimaryKey(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "卡片不存在");
        }

        // 1. 规范化输入：空串/纯空白 → null
        CardsDo patch = CardPatchNormalizer.normalize(rawCard);

        // 2. 唯一性校验：修改语义下排除自身 id
        //    注意：即使 URL/title 与自己相同也应允许（用户没改）；这一层排除由 Policy 内 id 相等判断处理
        uniquenessPolicy.validateUniqueness(id, patch.getUrl(), patch.getTitle());

        patch.setId(id);
        // 保留原 createdAt，不允许通过更新接口改创建时间
        patch.setCreatedAt(existing.getCreatedAt());
        patch.setUpdatedAt(new Date());
        // 注意：deletedAt 不在此处 set。
        // updateByPrimaryKeySelective 仅写非 null 字段，setDeletedAt(null) 不写 DB；
        // 删除卡片走 softDelete() 专用路径，与更新接口语义分离。
        cardsMapper.updateByPrimaryKeySelective(patch);
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
