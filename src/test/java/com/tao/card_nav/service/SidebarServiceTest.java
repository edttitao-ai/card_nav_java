package com.tao.card_nav.service;

import com.tao.card_nav.entity.SidebarDo;
import com.tao.card_nav.mapper.CardsDoMapper;
import com.tao.card_nav.mapper.SidebarDoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SidebarServiceTest {

    private SidebarDoMapper sidebarMapper;
    private CardsDoMapper cardsMapper;
    private SidebarService service;

    @BeforeEach
    void setUp() {
        sidebarMapper = mock(SidebarDoMapper.class);
        cardsMapper = mock(CardsDoMapper.class);
        service = new SidebarService(sidebarMapper, cardsMapper);
    }

    private static SidebarDo sidebar(String id, String label) {
        return SidebarDo.builder().id(id).label(label).sortOrder(0).build();
    }

    // ---------- resolveIdByLabel ----------

    @Test
    void resolveIdByLabel_exactMatch_returnsId() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                sidebar("tools", "工具"),
                sidebar("fav", "收藏")
        ));
        assertThat(service.resolveIdByLabel("工具")).isEqualTo("tools");
        assertThat(service.resolveIdByLabel("收藏")).isEqualTo("fav");
    }

    @Test
    void resolveIdByLabel_noMatch_returnsNull_noContainsFallback() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                sidebar("tools", "工具")
        ));
        assertThat(service.resolveIdByLabel("工")).isNull();   // 不再做 contains 兜底
        assertThat(service.resolveIdByLabel("other")).isNull();
    }

    @Test
    void resolveIdByLabel_nullLabel_returnsNull() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(sidebar("tools", "工具")));
        assertThat(service.resolveIdByLabel(null)).isNull();
    }

    @Test
    void resolveIdByLabel_emptyResult_returnsNull() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of());
        assertThat(service.resolveIdByLabel("anything")).isNull();
    }

    @Test
    void resolveIdByLabel_caseSensitive() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                sidebar("tools", "Tools")
        ));
        assertThat(service.resolveIdByLabel("tools")).isNull();  // 大小写敏感
        assertThat(service.resolveIdByLabel("Tools")).isEqualTo("tools");
    }

    @Test
    void resolveIdByLabel_skipsNullLabelInDb() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                SidebarDo.builder().id("a").label(null).build(),
                sidebar("tools", "工具")
        ));
        assertThat(service.resolveIdByLabel("工具")).isEqualTo("tools");
    }
}