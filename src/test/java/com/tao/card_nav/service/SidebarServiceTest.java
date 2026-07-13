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

/**
 * {@link SidebarService} 单测（mock {@link SidebarDoMapper} 与 {@link CardsDoMapper}）。
 *
 * <p>仅测 S-5 新增的 {@link SidebarService#resolveIdByLabel(String)}：
 * 验证"仅精确 equals 匹配、不做 contains 模糊兜底"的契约，与 CategoryService 同源。
 */
class SidebarServiceTest {

    private SidebarDoMapper sidebarMapper;
    @SuppressWarnings("unused") // SidebarService 构造依赖；本测试不直接调用
    private CardsDoMapper cardsMapper;
    private SidebarService service;

    @BeforeEach
    void setUp() {
        sidebarMapper = mock(SidebarDoMapper.class);
        cardsMapper = mock(CardsDoMapper.class);
        service = new SidebarService(sidebarMapper, cardsMapper);
    }

    /** 构造 id/label 的最小 SidebarDo，便于代码可读。 */
    private static SidebarDo sidebar(String id, String label) {
        return SidebarDo.builder().id(id).label(label).sortOrder(0).build();
    }

    // ============================================================
    // resolveIdByLabel
    // ============================================================

    /**
     * 精确匹配：返回对应 id。
     */
    @Test
    void resolveIdByLabel_exactMatch_returnsId() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                sidebar("tools", "工具"),
                sidebar("fav", "收藏")
        ));
        assertThat(service.resolveIdByLabel("工具")).isEqualTo("tools");
        assertThat(service.resolveIdByLabel("收藏")).isEqualTo("fav");
    }

    /**
     * <b>核心契约</b>：传 "工" 不应命中 "工具"。
     * 旧 VisitorsController 用 contains 兜底，重构后精确 equals。
     */
    @Test
    void resolveIdByLabel_noMatch_returnsNull_noContainsFallback() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                sidebar("tools", "工具")
        ));
        assertThat(service.resolveIdByLabel("工")).isNull();   // 不再做 contains 兜底
        assertThat(service.resolveIdByLabel("other")).isNull();
    }

    /**
     * 入参 label 为 null：返回 null，不抛异常。
     * 与 PATCH 语义保持一致（传 null = 不更新）。
     */
    @Test
    void resolveIdByLabel_nullLabel_returnsNull() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(sidebar("tools", "工具")));
        assertThat(service.resolveIdByLabel(null)).isNull();
    }

    /**
     * 侧边栏表为空：返回 null，不抛 NPE。
     */
    @Test
    void resolveIdByLabel_emptyResult_returnsNull() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of());
        assertThat(service.resolveIdByLabel("anything")).isNull();
    }

    /**
     * 大小写敏感：DB 里是 "Tools"，传 "tools" 不命中。
     */
    @Test
    void resolveIdByLabel_caseSensitive() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                sidebar("tools", "Tools")
        ));
        assertThat(service.resolveIdByLabel("tools")).isNull();  // 大小写敏感
        assertThat(service.resolveIdByLabel("Tools")).isEqualTo("tools");
    }

    /**
     * <b>防御性编程</b>：DB 里出现 label=null 的脏数据，不能抛 NPE。
     */
    @Test
    void resolveIdByLabel_skipsNullLabelInDb() {
        when(sidebarMapper.selectAllOrderBySortOrder()).thenReturn(List.of(
                SidebarDo.builder().id("a").label(null).build(),
                sidebar("tools", "工具")
        ));
        assertThat(service.resolveIdByLabel("工具")).isEqualTo("tools");
    }
}