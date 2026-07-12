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

class CardUniquenessPolicyTest {

    private CardsDoMapper mapper;
    private CardUniquenessPolicy policy;

    @BeforeEach
    void setUp() {
        mapper = mock(CardsDoMapper.class);
        policy = new CardUniquenessPolicy(mapper);
    }

    private static CardsDo cardWithId(long id, String title) {
        return CardsDo.builder().id(id).title(title).build();
    }

    // ---------- 双 null → 完全跳过 ----------

    @Test
    void validateUniqueness_bothNull_skipsMapper() {
        assertThatCode(() -> policy.validateUniqueness(null, null, null))
                .doesNotThrowAnyException();
        verify(mapper, never()).selectByUrl(anyString());
        verify(mapper, never()).selectByTitle(anyString());
    }

    // ---------- URL 独立分支 ----------

    @Test
    void validateUniqueness_urlConflictWithOther_throwsUrlConflict() {
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(999L, "Other"));
        assertThatThrownBy(() -> policy.validateUniqueness(null, "https://x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_URL_CONFLICT.getCode());
    }

    @Test
    void validateUniqueness_urlConflictWithSelf_passes() {
        // 修改语义下 URL 与自己原值相同 → 不算冲突
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(1L, "Self"));
        assertThatCode(() -> policy.validateUniqueness(1L, "https://x", null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateUniqueness_urlNoConflict_passes() {
        when(mapper.selectByUrl("https://x")).thenReturn(null);
        assertThatCode(() -> policy.validateUniqueness(null, "https://x", null))
                .doesNotThrowAnyException();
    }

    // ---------- Title 独立分支 ----------

    @Test
    void validateUniqueness_titleConflictWithOther_throwsTitleConflict() {
        when(mapper.selectByTitle("GH")).thenReturn(cardWithId(999L, "GH"));
        assertThatThrownBy(() -> policy.validateUniqueness(null, null, "GH"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_TITLE_CONFLICT.getCode());
    }

    @Test
    void validateUniqueness_titleConflictWithSelf_passes() {
        when(mapper.selectByTitle("GH")).thenReturn(cardWithId(1L, "GH"));
        assertThatCode(() -> policy.validateUniqueness(1L, null, "GH"))
                .doesNotThrowAnyException();
    }

    // ---------- 级联顺序：URL 命中 → 先抛 URL，不调 title ----------

    @Test
    void validateUniqueness_bothConflict_urlTakesPrecedence() {
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(999L, "Other"));
        // 不再 mock selectByTitle，验证 URL 优先抛出
        assertThatThrownBy(() -> policy.validateUniqueness(null, "https://x", "GH"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_URL_CONFLICT.getCode());
        verify(mapper, never()).selectByTitle(anyString());
    }

    // ---------- 独立分支各自调一次 mapper（无冲突） ----------

    @Test
    void validateUniqueness_bothFieldsProvidedNoConflict_callsBothMappersOnce() {
        when(mapper.selectByUrl(any())).thenReturn(null);
        when(mapper.selectByTitle(any())).thenReturn(null);

        policy.validateUniqueness(null, "https://x", "GH");

        verify(mapper, times(1)).selectByUrl("https://x");
        verify(mapper, times(1)).selectByTitle("GH");
    }

    @Test
    void validateUniqueness_nullCardIdExclude_addPathWorks() {
        // 新增路径下 excludeId = null；如果 mapper 命中 id=1L，仍然要抛错（不能误排除）
        when(mapper.selectByUrl("https://x")).thenReturn(cardWithId(1L, "Existing"));
        assertThatThrownBy(() -> policy.validateUniqueness(null, "https://x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_URL_CONFLICT.getCode());
    }

    @Test
    void validateUniqueness_excludeIdMismatch_stillThrows() {
        // 修改路径下 excludeId=5，命中冲突卡片 id=999 → 应当抛错
        when(mapper.selectByTitle("GH")).thenReturn(cardWithId(999L, "Other"));
        assertThatThrownBy(() -> policy.validateUniqueness(5L, null, "GH"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CARD_TITLE_CONFLICT.getCode());
    }
}