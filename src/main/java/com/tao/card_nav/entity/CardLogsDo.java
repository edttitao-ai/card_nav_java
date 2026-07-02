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
public class CardLogsDo {
    private Long id;

    private Long cardId;

    private String action;

    private String operatorIp;

    private Date createdAt;
}