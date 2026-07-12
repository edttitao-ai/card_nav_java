package com.tao.card_nav.domain;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;

/**
 * 卡片"补丁字段"规范化器：把外部传入的字段（REST / AI tool）归一为
 * "可以被 {@code updateByPrimaryKeySelective} / {@code insertSelective}
 * 正确消费的形态"。
 *
 * <p><b>语义契约</b>（PATCH 标准行为）：
 * <ul>
 *   <li>{@code null}：表示该字段"未提供"，selective mapper 会跳过 SET/INSERT，保留 DB 现有值。</li>
 *   <li>{@code ""} 或纯空白：归一为 {@code null}，同样跳过写入；避免把空串写进 DB 后产生空字符串 ≠ NULL。</li>
 *   <li>{@code "  内容  "}：trim 后写入。</li>
 * </ul>
 *
 * <p>本类的目的：消除"传了空串却没更新"的隐式行为，让 Service 不必再散落
 * {@code if (xxx != null && !xxx.isEmpty())} 这类防御性代码。
 *
 * <p>不可变单元（无状态工具类），可被 Service / Tool / Future 新写入路径共用。
 */
public final class CardPatchNormalizer {

    private CardPatchNormalizer() {}

    /**
     * 规范化输入，返回一个新的 {@link CardsDo} 作为可写快照。
     * 输入为 {@code null} 时直接抛 {@link BusinessException}（避免把 null 静默传到 mapper）。
     */
    public static CardsDo normalize(CardsDo input) {
        if (input == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "卡片数据不能为空");
        }
        CardsDo patch = new CardsDo();
        patch.setTitle(normalizeText(input.getTitle(), "title"));
        patch.setUrl(normalizeText(input.getUrl(), "url"));
        patch.setDescription(normalizeNullableText(input.getDescription()));
        patch.setExternalId(normalizeText(input.getExternalId(), "externalId"));
        patch.setSidebarId(normalizeText(input.getSidebarId(), "sidebarId"));
        patch.setCategoryId(input.getCategoryId());
        patch.setPinned(input.getPinned());
        patch.setFavicon(normalizeNullableText(input.getFavicon()));
        return patch;
    }

    /**
     * 必填文本字段的统一归一：null 保持 null；否则 trim，trim 后为空则归 null。
     * "必填"是业务层含义，本方法只负责 trim/空串→null，不做业务校验。
     */
    private static String normalizeText(String s, String fieldName) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 可空文本字段：null 保持 null；否则 trim，trim 后为空则归 null。
     * （与 normalizeText 等价，独立出来便于单测与语义标注。）
     */
    private static String normalizeNullableText(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}