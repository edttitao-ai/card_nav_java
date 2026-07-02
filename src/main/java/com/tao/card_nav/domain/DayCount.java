package com.tao.card_nav.domain;

/**
 * 每日统计结果
 */
public class DayCount {
    private String date;
    private Long count;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
