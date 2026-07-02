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
public class ClicksDo {
    private Long id;

    private String cardId;

    private Long count;

    private Date createdAt;

    private Date updatedAt;
}