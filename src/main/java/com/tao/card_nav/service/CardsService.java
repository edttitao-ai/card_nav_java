package com.tao.card_nav.service;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.mapper.CardsDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardsService {

    private final CardsDoMapper cardsMapper;

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
        return card;
    }

    /**
     * 生成唯一 externalId
     */
    private String generateExternalId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 更新卡片
     */
    public CardsDo updateCard(Long id, CardsDo card) {
        CardsDo existing = cardsMapper.selectByPrimaryKey(id);
        if (existing == null) {
            throw new BusinessException("卡片不存在");
        }
        card.setId(id);
        card.setUpdatedAt(new Date());
        // 不允许通过更新接口删除卡片
        card.setDeletedAt(null);
        cardsMapper.updateByPrimaryKeySelective(card);
        return cardsMapper.selectByPrimaryKey(id);
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
        return card;
    }
}
