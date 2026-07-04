package com.tao.card_nav.service;

import com.tao.card_nav.domain.CardLogWithCard;
import com.tao.card_nav.entity.CardLogsDo;
import com.tao.card_nav.mapper.CardLogsDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardLogsService {

    private final CardLogsDoMapper cardLogsMapper;

    /**
     * 查询某个卡片的所有操作日志
     */
    public List<CardLogsDo> getLogsByCardId(Long cardId) {
        return cardLogsMapper.selectByCardId(cardId);
    }

    /**
     * 查询某个卡片的所有操作日志（带卡片标题）
     */
    public List<CardLogWithCard> getLogsByCardIdWithTitle(Long cardId) {
        return cardLogsMapper.selectByCardIdWithTitle(cardId);
    }

    /**
     * 查询所有操作日志（带卡片标题、栏目名称、分类）
     */
    public List<CardLogWithCard> getAllLogs() {
        return cardLogsMapper.selectAll();
    }

    /**
     * 记录操作日志（AOP 调用）
     */
    public void logAction(Long cardId, String action, String operatorIp) {
        CardLogsDo log = CardLogsDo.builder()
                .cardId(cardId)
                .action(action)
                .operatorIp(operatorIp)
                .createdAt(new Date())
                .build();
        cardLogsMapper.insertSelective(log);
    }
}
