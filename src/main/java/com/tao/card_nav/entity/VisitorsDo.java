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
public class VisitorsDo {
    private Long id;

    private String ip;

    private String browser;

    private String device;

    private Date timestamp;
}