package com.tao.card_nav.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClickWithCard {
    private String cardId;
    private String cardTitle;
    private String sidebarId;
    private String sidebarLabel;
    private String category;
    private String favicon;
    private Long count;
}
