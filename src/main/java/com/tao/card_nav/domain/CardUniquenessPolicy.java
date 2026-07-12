package com.tao.card_nav.domain;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.mapper.CardsDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 卡片唯一性校验策略。
 *
 * <p>把"卡片 URL 全局唯一"和"卡片标题重复合规"两个独立校验从 Service 中抽离，
 * 单独模块化；Service / AI Tool 都可以调用，便于复用与单测。
 *
 * <p>两个校验相互独立，按 URL → Title 级联执行。每个分支独立抛
 * {@link BusinessException}，分别使用 {@link ErrorCode#CARD_URL_CONFLICT}
 * 与 {@link ErrorCode#CARD_TITLE_CONFLICT}，前端按 code 分流展示文案。
 *
 * <p>语义约定：
 * <ul>
 *   <li>URL / Title 都为 null（PATCH 不改这两个字段） → 跳过全部校验。</li>
 *   <li>URL / Title 与自己相同 → 不算冲突（修改语义下排除自身）。</li>
 *   <li>URL / Title 与他人相同 → 抛出对应 ErrorCode 的业务异常。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CardUniquenessPolicy {

    private final CardsDoMapper cardsMapper;

    /**
     * 校验唯一性。
     *
     * @param cardIdToExclude 修改语义下排除自身 id；新增传 {@code null}
     * @param url             规范化后的 URL（可为 null；null 表示不修改 URL，跳过 URL 校验）
     * @param title           规范化后的 title（可为 null；语义同上）
     * @throws BusinessException 冲突时抛错，code = {@link ErrorCode#CARD_URL_CONFLICT} 或
     *                           {@link ErrorCode#CARD_TITLE_CONFLICT}
     */
    public void validateUniqueness(Long cardIdToExclude, String url, String title) {
        // 独立分支 1：URL 全局唯一性
        checkUrlConflict(cardIdToExclude, url);

        // 独立分支 2：Title 重复合规（独立 if，逻辑完全解耦；级联顺序仅影响"先抛哪种错"）
        checkTitleConflict(cardIdToExclude, title);
    }

    private void checkUrlConflict(Long cardIdToExclude, String url) {
        if (url == null) {
            return;
        }
        CardsDo conflict = cardsMapper.selectByUrl(url);
        if (conflict != null && !conflict.getId().equals(cardIdToExclude)) {
            throw new BusinessException(ErrorCode.CARD_URL_CONFLICT,
                    "该链接已被卡片「" + conflict.getTitle() + "」(id=" + conflict.getId() + ")占用");
        }
    }

    private void checkTitleConflict(Long cardIdToExclude, String title) {
        if (title == null) {
            return;
        }
        CardsDo conflict = cardsMapper.selectByTitle(title);
        if (conflict != null && !conflict.getId().equals(cardIdToExclude)) {
            throw new BusinessException(ErrorCode.CARD_TITLE_CONFLICT,
                    "已存在同标题的卡片「" + conflict.getTitle() + "」(id=" + conflict.getId() + ")");
        }
    }
}