package com.tao.card_nav.domain;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.mapper.CardsDoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CardUniquenessPolicy} 单测（mock {@link CardsDoMapper}）。
 *
 * <p>测的核心契约：
 * <ul>
 *   <li>URL 与 Title 两个分支独立、各自抛对应 ErrorCode</li>
 *   <li>URL → Title 级联：URL 冲突时不查 title</li>
 *   <li>excludeId 用于"修改卡片时排除自身"——命中自己不算冲突</li>
 *   <li>两个字段都 null 时跳过全部校验（PATCH 语义：不改就不校验）</li>
 * </ul>
 */
class CardUniquenessPolicyTest {

    private CardsDoMapper mapper;
    private CardUniquenessPolicy policy;

    @BeforeEach
    void setUp() {
        mapper = mock(CardsDoMapper.class);
        policy = new CardUniquenessPolicy(mapper);
    }

    /** 构造带 id 与 title 的测试卡片，便于代码可读。 */
    private static CardsDo cardWithId(long id, String title) {
        return CardsDo.builder().id(id).title(title).build();
    }

    // ---------- 双 null → 完全跳过 ----------

    /**
     * url 与 title 都为 null：PATCH 语义意味着"不改这两个字段"，
     * Policy 必须不调 mapper、不抛异常直接通过。
     */
    @Test
    void validateUniqueness_bothNull_skipsMapper() {
        assertThatCode(() -> policy.validateUniqueness(null, null, null))
                .doesNotThrowAnyException();
        // 双重 verify：URL 与 Title 校验都被跳过（节省一次 SELECT）
        verify(mapper, never()).selectByUrl(anyString());
        verify(mapper, never()).selectByTitle(anyString());
    }

    // ---------- URL 独立分支 ----------

    /**
     * URL 与他人卡片冲突：抛 CARD_URL_CONFLICT，不抛 CARD_TITLE_CONFLICT。
     */
    @Test
    void validateUniqueness_urlConflictWithOther_throwsUrlConflict() {
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(999L, "Other"));
        assertThatThrownBy(() -> policy.validateUniqueness(null, "https://x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_URL_CONFLICT.getCode());
    }

    /**
     * URL 与自己原值相同：不算冲突（修改语义下"用户没改 URL"必须允许）。
     * 此处 excludeId=1L，mapper 命中 id=1L → Policy 内部应识别为自身、跳过。
     */
    @Test
    void validateUniqueness_urlConflictWithSelf_passes() {
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(1L, "Self"));
        assertThatCode(() -> policy.validateUniqueness(1L, "https://x", null))
                .doesNotThrowAnyException();
    }

    /**
     * URL 没有任何匹配：放行。
     */
    @Test
    void validateUniqueness_urlNoConflict_passes() {
        when(mapper.selectByUrl("https://x")).thenReturn(null);
        assertThatCode(() -> policy.validateUniqueness(null, "https://x", null))
                .doesNotThrowAnyException();
    }

    // ---------- Title 独立分支 ----------

    /**
     * Title 与他人冲突：抛 CARD_TITLE_CONFLICT（与 URL 错误码不同，前端按 code 分流）。
     */
    @Test
    void validateUniqueness_titleConflictWithOther_throwsTitleConflict() {
        when(mapper.selectByTitle("GH")).thenReturn(cardWithId(999L, "GH"));
        assertThatThrownBy(() -> policy.validateUniqueness(null, null, "GH"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_TITLE_CONFLICT.getCode());
    }

    /**
     * Title 与自己原值相同：放行。
     */
    @Test
    void validateUniqueness_titleConflictWithSelf_passes() {
        when(mapper.selectByTitle("GH")).thenReturn(cardWithId(1L, "GH"));
        assertThatCode(() -> policy.validateUniqueness(1L, null, "GH"))
                .doesNotThrowAnyException();
    }

    // ---------- 级联顺序：URL 命中 → 先抛 URL，不调 title ----------

    /**
     * URL 与 Title 都冲突：URL 分支先抛错，且 Policy 不应再调 selectByTitle。
     * 这是"性能 + 一致性"——早失败避免多余 IO。
     */
    @Test
    void validateUniqueness_bothConflict_urlTakesPrecedence() {
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(999L, "Other"));
        // 故意不 mock selectByTitle —— 一旦被调用会因 "unstubbed method" 失败
        assertThatThrownBy(() -> policy.validateUniqueness(null, "https://x", "GH"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_URL_CONFLICT.getCode());
        verify(mapper, never()).selectByTitle(anyString());
    }

    // ---------- 独立分支各自调一次 mapper（无冲突） ----------

    /**
     * URL 与 Title 都提供了、都无冲突：mapper 各自被调一次，
     * verify 保证"没多余调用"——避免无谓 IO。
     */
    @Test
    void validateUniqueness_bothFieldsProvidedNoConflict_callsBothMappersOnce() {
        when(mapper.selectByUrl(any())).thenReturn(null);
        when(mapper.selectByTitle(any())).thenReturn(null);

        policy.validateUniqueness(null, "https://x", "GH");

        verify(mapper, times(1)).selectByUrl("https://x");
        verify(mapper, times(1)).selectByTitle("GH");
    }

    /**
     * 新增路径（excludeId=null）：即使 mapper 命中 id=1L，也必须抛错。
     * 防止 null excludeId 被误处理成"排除所有"的 bug。
     */
    @Test
    void validateUniqueness_nullCardIdExclude_addPathWorks() {
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(1L, "Existing"));
        assertThatThrownBy(() -> policy.validateUniqueness(null, "https://x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_URL_CONFLICT.getCode());
    }

    /**
     * 修改路径下 excludeId=5，命中冲突卡片 id=999：必须抛错
     * （即"自己的 id=5"才算自身排除）。
     */
    @Test
    void validateUniqueness_excludeIdMismatch_stillThrows() {
        when(mapper.selectByTitle("GH")).thenReturn(cardWithId(999L, "Other"));
        assertThatThrownBy(() -> policy.validateUniqueness(5L, null, "GH"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_TITLE_CONFLICT.getCode());
    }
}