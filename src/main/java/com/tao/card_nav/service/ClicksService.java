package com.tao.card_nav.service;

import com.tao.card_nav.domain.ClickWithCard;
import com.tao.card_nav.entity.ClicksDo;
import com.tao.card_nav.mapper.ClicksDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClicksService {

    private final ClicksDoMapper clicksMapper;

    /**
     * 点击卡片：如果已存在则 +1，否则新增
     */
    public void clickCard(String cardId) {
        ClicksDo existing = clicksMapper.selectByCardId(cardId);
        if (existing != null) {
            // 已存在，+1
            clicksMapper.incrementCount(cardId);
        } else {
            // 不存在，新增
            ClicksDo click = ClicksDo.builder()
                    .cardId(cardId)
                    .count(1L)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
            clicksMapper.insertSelective(click);
        }
    }

    /**
     * 获取点击排行榜 TOP N（联表查卡片信息）
     */
    public List<ClickWithCard> getTopClicks(int limit) {
        List<ClickWithCard> all = clicksMapper.selectAllWithCard();
        if (all.size() > limit) {
            return all.subList(0, limit);
        }
        return all;
    }

    /**
     * 按 cardId 查询点击记录
     */
    public ClicksDo getClickByCardId(String cardId) {
        return clicksMapper.selectByCardId(cardId);
    }
}
