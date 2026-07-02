package com.tao.card_nav.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FavoritesDo {
    private Long id;
    private Long cardId;
    private String title;
    private String url;
    private String description;
    private String category;
    private String favicon;
    private Date createdAt;
}