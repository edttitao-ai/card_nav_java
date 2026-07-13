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

class CardDeleteHistoryServiceTest {

    private CardDeleteHistoryDoMapper mapper;
    private CardDeleteHistoryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CardDeleteHistoryDoMapper.class);
        service = new CardDeleteHistoryService(mapper);
    }

    // ---------- recordRequest ----------

    @Test
    void recordRequest_insertsRecord() {
        service.recordRequest(42L, "GitHub", "AI 申请", "127.0.0.1",
                CardDeleteHistoryDo.Status.REQUESTED);

        verify(mapper, times(1)).insertSelective(any(CardDeleteHistoryDo.class));
    }

    @Test
    void recordRequest_nullStatus_defaultsToRequested() {
        service.recordRequest(42L, "GitHub", "AI 申请", "127.0.0.1", null);

        org.mockito.ArgumentCaptor<CardDeleteHistoryDo> captor =
                org.mockito.ArgumentCaptor.forClass(CardDeleteHistoryDo.class);
        verify(mapper).insertSelective(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus())
                .isEqualTo(CardDeleteHistoryDo.Status.REQUESTED);
    }

    @Test
    void recordRequest_mapperThrows_doesNotPropagate() {
        when(mapper.insertSelective(any())).thenThrow(new RuntimeException("db down"));

        // 写历史失败不应阻断主业务（删除流程已成功）
        org.assertj.core.api.Assertions.assertThatCode(() ->
                service.recordRequest(42L, "GitHub", "AI 申请", "127.0.0.1",
                        CardDeleteHistoryDo.Status.REQUESTED)
        ).doesNotThrowAnyException();
    }

    // ---------- listRecent ----------

    @Test
    void listRecent_passesThroughToMapper() {
        when(mapper.selectAllOrderByCreatedAtDesc(50)).thenReturn(List.of());
        service.listRecent(null);
        verify(mapper, times(1)).selectAllOrderByCreatedAtDesc(50);
    }

    @Test
    void listRecent_zeroOrNegative_usesDefault() {
        when(mapper.selectAllOrderByCreatedAtDesc(50)).thenReturn(List.of());
        service.listRecent(0);
        service.listRecent(-1);
        verify(mapper, times(2)).selectAllOrderByCreatedAtDesc(50);
    }

    @Test
    void listRecent_positiveValue_passesAsIs() {
        when(mapper.selectAllOrderByCreatedAtDesc(10)).thenReturn(List.of());
        service.listRecent(10);
        verify(mapper, times(1)).selectAllOrderByCreatedAtDesc(10);
    }
}