package com.tao.card_nav.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SidebarDo {
    private String id;

    private String label;

    private String icon;

    private Integer sortOrder;
}