package com.tao.card_nav.service;

import com.tao.card_nav.entity.CategoryDo;
import com.tao.card_nav.mapper.CategoryDoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CategoryService} 单测（mock {@link CategoryDoMapper}）。
 *
 * <p>仅测 S-5 新增的 {@link CategoryService#resolveIdByName(String)}：
 * 验证"仅精确 equals 匹配、不做 contains 模糊兜底"的契约。
 */
class CategoryServiceTest {

    private CategoryDoMapper mapper;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CategoryDoMapper.class);
        service = new CategoryService(mapper);
    }

    /** 构造 id/name 的最小 CategoryDo，便于代码可读。 */
    private static CategoryDo cat(Long id, String name) {
        return CategoryDo.builder().id(id).name(name).sortOrder(0).build();
    }

    // ============================================================
    // resolveIdByName
    // ============================================================

    /**
     * 精确匹配：返回对应 id。
     */
    @Test
    void resolveIdByName_exactMatch_returnsId() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                cat(1L, "AI 写作工具"),
                cat(2L, "AI 编程工具")
        ));
        assertThat(service.resolveIdByName("AI 写作工具")).isEqualTo(1L);
    }

    /**
     * <b>核心契约</b>：传 "AI" 不应命中 "AI 写作工具"。
     * 旧 VisitorsController 用 contains 兜底导致 "AI" 误匹配 → 静默错误；
     * 重构后精确 equals，"AI" 与 "AI 写作" 都返回 null。
     */
    @Test
    void resolveIdByName_noMatch_returnsNull() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                cat(1L, "AI 写作工具")
        ));
        assertThat(service.resolveIdByName("AI")).isNull();        // 不再 contains 兜底
        assertThat(service.resolveIdByName("AI 写作")).isNull();    // 子串也不命中
    }

    /**
     * 入参 name 为 null：返回 null，不抛异常。
     * 这是"传了=null 即不更新"的 PATCH 语义要求——校验逻辑必须能容忍。
     */
    @Test
    void resolveIdByName_nullName_returnsNull() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(cat(1L, "x")));
        assertThat(service.resolveIdByName(null)).isNull();
    }

    /**
     * 分类表为空（DB 还没初始化 / 暂时没分类）：返回 null，不抛 NPE。
     */
    @Test
    void resolveIdByName_emptyResult_returnsNull() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of());
        assertThat(service.resolveIdByName("anything")).isNull();
    }

    /**
     * 大小写敏感：DB 里是 "GitHub"，传 "github" 不命中。
     * 注意：当前实现不区分大小写——与 S-5 审查报告精神一致。
     */
    @Test
    void resolveIdByName_caseSensitive() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                cat(1L, "GitHub")
        ));
        assertThat(service.resolveIdByName("github")).isNull(); // 大小写敏感
        assertThat(service.resolveIdByName("GitHub")).isEqualTo(1L);
    }

    /**
     * <b>防御性编程</b>：DB 里出现 name=null 的脏数据（虽然不该出现），
     * resolveIdByName 不能抛 NPE。
     */
    @Test
    void resolveIdByName_skipsNullNameInDb() {
        when(mapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                CategoryDo.builder().id(1L).name(null).build(),
                cat(2L, "OK")
        ));
        assertThat(service.resolveIdByName("OK")).isEqualTo(2L);
    }
}