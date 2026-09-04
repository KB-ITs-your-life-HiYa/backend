package com.fledge.care.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.ingest.GeminiClient;
import com.fledge.care.dto.CareDto.Choice;
import com.fledge.care.dto.CareDto.FreeTextRequest;
import com.fledge.care.dto.CareDto.Summary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CareGeminiServiceTest {
    private final CareService care = mock(CareService.class);
    private final GeminiClient gemini = mock(GeminiClient.class);
    private final CareGeminiService service = new CareGeminiService(care, gemini, new ObjectMapper());
    private final Summary summary = mock(Summary.class);

    @Test
    void validJsonCompletesThePreparedResponse() throws Exception {
        var request = new FreeTextRequest("적금 납입이 어려워요", "request-1");
        var prepared = preparation(true);
        when(care.prepareFreeText(2L, 3L, request)).thenReturn(prepared);
        when(gemini.generateJson(anyString(), anyString(), eq(512)))
                .thenReturn("{\"choice\":\"DIFFICULT\",\"reply\":\"생활비를 먼저 살펴봐요.\"}");
        when(care.completeFreeText(2L, 3L, 7L, Choice.DIFFICULT, "생활비를 먼저 살펴봐요."))
                .thenReturn(summary);

        assertThat(service.message(2L, 3L, request)).isSameAs(summary);
        verify(care).completeFreeText(2L, 3L, 7L, Choice.DIFFICULT, "생활비를 먼저 살펴봐요.");
        verify(care, never()).failFreeText(anyLong(), anyLong(), anyLong());
    }

    @Test
    void malformedGeminiResponsePreservesInputAsError() throws Exception {
        var request = new FreeTextRequest("도움이 필요해요", "request-2");
        when(care.prepareFreeText(2L, 3L, request)).thenReturn(preparation(true));
        when(gemini.generateJson(anyString(), anyString(), eq(512))).thenReturn("not-json");
        when(care.failFreeText(2L, 3L, 7L)).thenReturn(summary);

        assertThat(service.message(2L, 3L, request)).isSameAs(summary);
        verify(care).failFreeText(2L, 3L, 7L);
        verify(care, never()).completeFreeText(anyLong(), anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void duplicateRequestReturnsStoredConversationWithoutCallingGemini() {
        var request = new FreeTextRequest("같은 요청", "request-3");
        when(care.prepareFreeText(2L, 3L, request)).thenReturn(preparation(false));
        when(care.summary(2L)).thenReturn(summary);

        assertThat(service.message(2L, 3L, request)).isSameAs(summary);
        verifyNoInteractions(gemini);
    }

    private CareService.GeminiPreparation preparation(boolean shouldGenerate) {
        return new CareService.GeminiPreparation(7L, shouldGenerate, "MISSED_SAVING", "시연 정기적금",
                LocalDate.of(2026, 9, 23), 200000L, "적금 납입이 어려워요", List.of());
    }
}
