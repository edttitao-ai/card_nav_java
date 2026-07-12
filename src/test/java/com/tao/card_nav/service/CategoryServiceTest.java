package com.tao.card_nav.service;

import com.tao.card_nav.entity.CategoryDo;
import com.tao.card_nav.mapper.CategoryDoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private CategoryDoMapper mapper;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CategoryDoMapper.class);
        service = new CategoryService(mapper);
    }

    private static CategoryDo cat(Long id, String name) {
        return CategoryDo.builder().id(id).name(name).sortOrder(0).build();
    }

    // ---------- resolveIdByName ----------

    @Test
    void resolveIdByName_exactMatch_returnsId() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                cat(1L, "AI 写作工具"),
                cat(2L, "AI 编程工具")
        ));
        assertThat(service.resolveIdByName("AI 写作工具")).isEqualTo(1L);
    }

    @Test
    void resolveIdByName_noMatch_returnsNull() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                cat(1L, "AI 写作工具")
        ));
        assertThat(service.resolveIdByName("AI")).isNull();        // 不再 contains 兜底
        assertThat(service.resolveIdByName("AI 写作")).isNull();    // 子串也不命中
    }

    @Test
    void resolveIdByName_nullName_returnsNull() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(cat(1L, "x")));
        assertThat(service.resolveIdByName(null)).isNull();
    }

    @Test
    void resolveIdByName_emptyResult_returnsNull() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of());
        assertThat(service.resolveIdByName("anything")).isNull();
    }

    @Test
    void resolveIdByName_caseSensitive() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                cat(1L, "GitHub")
        ));
        assertThat(service.resolveIdByName("github")).isNull(); // 大小写敏感
        assertThat(service.resolveIdByName("GitHub")).isEqualTo(1L);
    }

    @Test
    void resolveIdByName_skipsNullNameInDb() {
        // DB 里出现 name=null 的脏数据，不能抛 NPE
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                CategoryDo.builder().id(1L).name(null).build(),
                cat(2L, "OK")
        ));
        assertThat(service.resolveIdByName("OK")).isEqualTo(2L);
    }
}