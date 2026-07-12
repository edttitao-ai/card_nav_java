package com.tao.card_nav.domain;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardPatchNormalizerTest {

    // ---------- 入参整体为 null ----------

    @Test
    void normalize_nullInput_throwsBusinessException() {
        assertThatThrownBy(() -> CardPatchNormalizer.normalize(null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    // ---------- 全部字段为 null / 缺省 ----------

    @Test
    void normalize_allFieldsNull_returnsAllNull() {
        CardsDo in = new CardsDo();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getTitle()).isNull();
        assertThat(out.getUrl()).isNull();
        assertThat(out.getDescription()).isNull();
        assertThat(out.getExternalId()).isNull();
        assertThat(out.getSidebarId()).isNull();
        assertThat(out.getFavicon()).isNull();
        assertThat(out.getCategoryId()).isNull();
        assertThat(out.getPinned()).isNull();
    }

    // ---------- 文本字段 trim + 空串归 null ----------

    @Test
    void normalize_urlTrimmed() {
        CardsDo in = CardsDo.builder().url("  https://github.com  ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getUrl()).isEqualTo("https://github.com");
    }

    @Test
    void normalize_urlEmptyString_becomesNull() {
        CardsDo in = CardsDo.builder().url("").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getUrl()).isNull();
    }

    @Test
    void normalize_urlWhitespaceOnly_becomesNull() {
        CardsDo in = CardsDo.builder().url("   ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getUrl()).isNull();
    }

    @Test
    void normalize_titleTrimmed() {
        CardsDo in = CardsDo.builder().title("  GitHub  ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getTitle()).isEqualTo("GitHub");
    }

    @Test
    void normalize_titleEmpty_becomesNull() {
        CardsDo in = CardsDo.builder().title("").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getTitle()).isNull();
    }

    @Test
    void normalize_descriptionWhitespace_becomesNull() {
        CardsDo in = CardsDo.builder().description("   ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getDescription()).isNull();
    }

    @Test
    void normalize_descriptionKeepsValue() {
        CardsDo in = CardsDo.builder().description("  hello  ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getDescription()).isEqualTo("hello");
    }

    // ---------- 透传字段 ----------

    @Test
    void normalize_pinnedCategoryIdPassedThrough() {
        CardsDo in = CardsDo.builder().pinned(true).categoryId(7L).build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getPinned()).isTrue();
        assertThat(out.getCategoryId()).isEqualTo(7L);
    }

    // ---------- 不可变性 ----------

    @Test
    void normalize_returnsNewInstance() {
        CardsDo in = CardsDo.builder().title("x").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out).isNotSameAs(in);
    }
}