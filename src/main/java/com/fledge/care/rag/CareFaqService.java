package com.fledge.care.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.ingest.GeminiClient;
import com.fledge.care.dto.CareDto.FaqAskResponse;
import com.fledge.care.dto.CareDto.FaqSource;
import com.fledge.care.rag.RagRetriever.Match;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 흐름: 질문 → docs/rag 에서 관련 청크 검색 → 그 내용을 프롬프트에 넣음 → Gemini가 그 근거로만 답변.
 *
 * 지원금 / 독립지원(주거) / 자립동행 서비스 이용 방법, 이 세 주제만 다룬다. 그 외 질문이거나
 * 근거 문서가 없으면 "답변이 어렵다"고 답한다. Care 상담(금융 이상징후, CareGeminiService)이나
 * 정책 카드(CarePolicyService)는 건드리지 않는다 — 그쪽은 그대로 규칙·데이터 기반으로 둔다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareFaqService {

    private static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "inScope": {"type": "boolean"},
                "found": {"type": "boolean"},
                "answer": {"type": "string"}
              },
              "required": ["inScope", "found", "answer"]
            }
            """;

    private static final String OUT_OF_SCOPE_MESSAGE =
            "죄송해요, 저는 지원금·독립지원(주거)·자립동행 서비스 이용 관련 질문에만 답변드릴 수 있어요. "
                    + "그 외 질문은 답변이 어려워요.";

    private static final String NOT_FOUND_MESSAGE =
            "죄송해요, 지금 갖고 있는 자료에서는 관련 내용을 찾지 못했어요. 정확한 내용은 담당 상담사에게 확인해 주세요.";

    // 청크 몇 개까지 프롬프트에 넣을지. 너무 많이 넣으면 답이 늘어지고 근거도 흐려진다
    @Value("${rag.top-k:3}")
    private int topK;

    private final RagKnowledgeBase knowledgeBase;
    private final RagRetriever retriever;
    private final GeminiClient gemini;
    private final ObjectMapper mapper;

    public FaqAskResponse ask(String question) {
        List<Match> matches = retriever.topMatches(question, knowledgeBase.chunks(), topK);
        if (matches.isEmpty()) {
            // 지원금/독립지원/서비스 문서 전체와 겹치는 키워드가 하나도 없다 — 범위 밖 질문일 확률이 높다.
            log.info("RAG 근거 문서 없음: question={}", question);
            return outOfScope();
        }

        try {
            GeminiAnswer result = mapper.readValue(
                    gemini.generateJson(prompt(question, matches), RESPONSE_SCHEMA, 1024), GeminiAnswer.class);
            if (!result.inScope()) return outOfScope();
            if (!result.found() || result.answer() == null || result.answer().isBlank()) {
                // 청크는 키워드상 걸렸지만 실제로는 질문에 답이 안 되는 내용일 수 있다.
                // Gemini가 스스로 found=false 로 보고하게 해서 억지 답변(환각)을 막는다.
                return notFound();
            }
            List<FaqSource> sources = matches.stream()
                    .map(m -> new FaqSource(m.chunk().docId(), m.chunk().title()))
                    .toList();
            return new FaqAskResponse(result.answer().trim(), true, sources);
        } catch (Exception e) {
            log.warn("RAG 답변 생성 실패: question={}, cause={}", question, e.getClass().getSimpleName());
            return new FaqAskResponse("지금 답변을 생성하는 데 문제가 생겼어요. 잠시 후 다시 시도해 주세요.", false, List.of());
        }
    }

    private static FaqAskResponse outOfScope() {
        return new FaqAskResponse(OUT_OF_SCOPE_MESSAGE, false, List.of());
    }

    private static FaqAskResponse notFound() {
        return new FaqAskResponse(NOT_FOUND_MESSAGE, false, List.of());
    }

    private String prompt(String question, List<Match> matches) {
        StringBuilder context = new StringBuilder();
        for (Match match : matches) {
            context.append(match.chunk().toPromptBlock()).append("\n\n");
        }
        return """
                당신은 자립동행 서비스의 안내 챗봇입니다. 아래 "참고 문서"는 우리 팀이 직접 정리했거나
                공공 API에서 받아온 자료이며, 사용자 질문에 답할 유일한 근거입니다.

                이 챗봇은 다음 세 주제만 다룹니다: (1) 지원금(정부/지자체 지원 제도), (2) 독립지원(주거·임대주택),
                (3) 자립동행 서비스 이용 방법. 질문이 이 세 주제와 관련 없으면 inScope=false 로 답하고
                found=false, answer="" 로 두세요. 금융 이상징후 상담이나 개인화된 자격 판정처럼 사람이 직접
                판단해야 하는 내용도 이 경로에서 다루지 않으므로 inScope=false 로 답하세요.

                주제 범위 안이면 inScope=true 로 하고 아래 규칙을 따르세요:
                - 참고 문서에 실제로 적힌 내용만 근거로 답하세요. 문서에 없는 조건·금액·날짜를 추측하거나 지어내지 마세요.
                - 참고 문서가 질문에 실제로 답이 되면 found=true, answer에 한국어 2~4문장으로 간결하게 답하세요.
                - 참고 문서가 질문과 관련은 있어 보여도 실제 답이 되지 않으면 found=false로 답하고 answer는 빈 문자열로 두세요.
                - 정책 정보는 바뀔 수 있으니, found=true 답변의 마지막에는 "정확한 조건은 담당 기관에서 다시 확인해 주세요." 를 덧붙이세요.

                참고 문서:
                """ + context + """

                사용자 질문: """ + question;
    }

    private record GeminiAnswer(boolean inScope, boolean found, String answer) {}
}
