package com.tao.card_nav.domain;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CardPatchNormalizer} 单测。
 *
 * <p>测的核心契约：
 * <ul>
 *   <li>空串 / 纯空白 → null（PATCH 标准：null=不更新）</li>
 *   <li>文本字段 trim</li>
 *   <li>入参 null 抛 {@link BusinessException(PARAMS_ERROR)}</li>
 *   <li>返回的是新对象，不改原 input</li>
 * </ul>
 */
class CardPatchNormalizerTest {

    // ---------- 入参整体为 null ----------

    /**
     * 入参 null：必须立即抛 {@link BusinessException}（PARAMS_ERROR），
     * 不允许把 null 静默传到 mapper。
     */
    @Test
    void normalize_nullInput_throwsBusinessException() {
        assertThatThrownBy(() -> CardPatchNormalizer.normalize(null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    // ---------- 全部字段为 null / 缺省 ----------

    /**
     * 入参 CardsDo 全字段 null / 缺省：输出所有字段仍为 null，
     * 没有"空字符串默认值"等意外副作用。
     */
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

    /**
     * 文本字段两端空格必须被 trim；保证写入 DB 后字符串无前后空白。
     */
    @Test
    void normalize_urlTrimmed() {
        CardsDo in = CardsDo.builder().url("  https://github.com  ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getUrl()).isEqualTo("https://github.com");
    }

    /**
     * 空字符串 → null：selective mapper 不会写入 NULL，相当于"不更新"语义，
     * 避免空串进 DB 后产生空字符串字段。
     */
    @Test
    void normalize_urlEmptyString_becomesNull() {
        CardsDo in = CardsDo.builder().url("").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getUrl()).isNull();
    }

    /**
     * 纯空白 → null：与空字符串同样归一化处理。
     */
    @Test
    void normalize_urlWhitespaceOnly_becomesNull() {
        CardsDo in = CardsDo.builder().url("   ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getUrl()).isNull();
    }

    /**
     * title 同 URL：trim 归一。
     */
    @Test
    void normalize_titleTrimmed() {
        CardsDo in = CardsDo.builder().title("  GitHub  ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getTitle()).isEqualTo("GitHub");
    }

    /**
     * title 空串 → null。
     */
    @Test
    void normalize_titleEmpty_becomesNull() {
        CardsDo in = CardsDo.builder().title("").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getTitle()).isNull();
    }

    /**
     * description 纯空白 → null（description 是可空字段，与 URL/title 同语义）。
     */
    @Test
    void normalize_descriptionWhitespace_becomesNull() {
        CardsDo in = CardsDo.builder().description("   ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getDescription()).isNull();
    }

    /**
     * description 有真实内容时保留 trim 后的值。
     */
    @Test
    void normalize_descriptionKeepsValue() {
        CardsDo in = CardsDo.builder().description("  hello  ").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getDescription()).isEqualTo("hello");
    }

    // ---------- 透传字段 ----------

    /**
     * 非文本字段（pinned / categoryId）原样透传——Normalizer 不应做隐式转换。
     */
    @Test
    void normalize_pinnedCategoryIdPassedThrough() {
        CardsDo in = CardsDo.builder().pinned(true).categoryId(7L).build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out.getPinned()).isTrue();
        assertThat(out.getCategoryId()).isEqualTo(7L);
    }

    // ---------- 不可变性 ----------

    /**
     * Normalizer 必须返回新对象，绝不能原地修改入参（避免业务侧拿到 input 时已被污染）。
     */
    @Test
    void normalize_returnsNewInstance() {
        CardsDo in = CardsDo.builder().title("x").build();
        CardsDo out = CardPatchNormalizer.normalize(in);
        assertThat(out).isNotSameAs(in);
    }
}