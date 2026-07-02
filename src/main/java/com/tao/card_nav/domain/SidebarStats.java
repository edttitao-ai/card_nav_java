package com.tao.card_nav.domain;

/**
 * 栏目统计结果
 */
public class SidebarStats {
    private String name;
    private String sidebarId;
    private Long cardCount;
    private Long clickCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSidebarId() {
        return sidebarId;
    }

    public void setSidebarId(String sidebarId) {
        this.sidebarId = sidebarId;
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