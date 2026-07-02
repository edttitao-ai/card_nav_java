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
public class CardsDo {
    private Long id;

    private String externalId;

    private String title;

    private String url;

    private String description;

    private Long categoryId;

    private String category;   // 联表查出 category.name

    private String sidebarId;

    private String favicon;

    private Boolean pinned;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;
}