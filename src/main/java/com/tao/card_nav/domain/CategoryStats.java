package com.tao.card_nav.domain;

/**
 * 分类统计结果
 */
public class CategoryStats {
    private String name;
    private Long cardCount;
    private Long clickCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCardCount() {
        return cardCount;
    }

    public void setCardCount(Long cardCount) {
        this.cardCount = cardCount;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }
}