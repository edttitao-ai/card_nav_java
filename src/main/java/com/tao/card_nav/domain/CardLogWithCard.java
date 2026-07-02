package com.tao.card_nav.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardLogWithCard {
    private Long id;
    private Long cardId;
    private String cardTitle;
    private String action;
    private String operatorIp;
    private Date createdAt;
    private String sidebarLabel; // 关联 sidebar 表的 label
    private String category;    // 关联 category 表的 name
}
