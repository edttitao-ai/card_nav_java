package com.tao.card_nav.service;

import com.tao.card_nav.entity.CardDeleteHistoryDo;
import com.tao.card_nav.mapper.CardDeleteHistoryDoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CardDeleteHistoryService} 单测（mock {@link CardDeleteHistoryDoMapper}）。
 *
 * <p>测的核心契约：
 * <ul>
 *   <li>recordRequest 写入历史；status 默认 REQUESTED</li>
 *   <li><b>写历史失败不应阻断主业务</b>——service 内 try/catch 吞异常</li>
 *   <li>listRecent 默认 limit=50；null / 0 / 负数都按 50 处理</li>
 * </ul>
 */
class CardDeleteHistoryServiceTest {

    private CardDeleteHistoryDoMapper mapper;
    private CardDeleteHistoryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CardDeleteHistoryDoMapper.class);
        service = new CardDeleteHistoryService(mapper);
    }

    // ============================================================
    // recordRequest
    // ============================================================

    /**
     * 正常路径：recordRequest 调一次 mapper.insertSelective。
     */
    @Test
    void recordRequest_insertsRecord() {
        service.recordRequest(42L, "GitHub", "AI 申请", "127.0.0.1",
                CardDeleteHistoryDo.Status.REQUESTED);

        verify(mapper, times(1)).insertSelective(any(CardDeleteHistoryDo.class));
    }

    /**
     * status 传 null 时：service 内部默认填 REQUESTED。
     * 验证 Service 内部确实走了默认填充分支（防止上游忘了传 status）。
     */
    @Test
    void recordRequest_nullStatus_defaultsToRequested() {
        service.recordRequest(42L, "GitHub", "AI 申请", "127.0.0.1", null);

        org.mockito.ArgumentCaptor<CardDeleteHistoryDo> captor =
                org.mockito.ArgumentCaptor.forClass(CardDeleteHistoryDo.class);
        verify(mapper).insertSelective(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus())
                .isEqualTo(CardDeleteHistoryDo.Status.REQUESTED);
    }

    /**
     * <b>核心契约</b>：DB 写历史失败不应向上抛异常。
     * 删除流程已经成功（Redis 已消费、卡片已软删除），如果历史表写挂了，
     * 不应让用户看到 500——日志记录即可。
     */
    @Test
    void recordRequest_mapperThrows_doesNotPropagate() {
        when(mapper.insertSelective(any())).thenThrow(new RuntimeException("db down"));

        org.assertj.core.api.Assertions.assertThatCode(() ->
                service.recordRequest(42L, "GitHub", "AI 申请", "127.0.0.1",
                        CardDeleteHistoryDo.Status.REQUESTED)
        ).doesNotThrowAnyException();
    }

    // ============================================================
    // listRecent
    // ============================================================

    /**
     * limit=null：传默认 50 给 mapper（避免 null 流到 SQL）。
     */
    @Test
    void listRecent_passesThroughToMapper() {
        when(mapper.selectAllOrderByCreatedAtDesc(50)).thenReturn(List.of());
        service.listRecent(null);
        verify(mapper, times(1)).selectAllOrderByCreatedAtDesc(50);
    }

    /**
     * limit=0 或负数：仍按 50 处理（与 null 一致）。
     * 一次断言里调两次，验证幂等。
     */
    @Test
    void listRecent_zeroOrNegative_usesDefault() {
        when(mapper.selectAllOrderByCreatedAtDesc(50)).thenReturn(List.of());
        service.listRecent(0);
        service.listRecent(-1);
        verify(mapper, times(2)).selectAllOrderByCreatedAtDesc(50);
    }

    /**
     * limit > 0：原值传给 mapper，不做截断。
     */
    @Test
    void listRecent_positiveValue_passesAsIs() {
        when(mapper.selectAllOrderByCreatedAtDesc(10)).thenReturn(List.of());
        service.listRecent(10);
        verify(mapper, times(1)).selectAllOrderByCreatedAtDesc(10);
    }
}