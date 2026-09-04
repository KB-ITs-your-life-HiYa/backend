package com.fledge.benefit.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.domain.Subsidy;
import com.fledge.benefit.domain.SubsidyRaw;
import com.fledge.benefit.repository.SubsidyRawRepository;
import com.fledge.benefit.repository.SubsidyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Profile("backfill-category")
public class CategoryBackfillRunner implements CommandLineRunner {

    private static final int BATCH_SIZE = 15;

    private static final String CATEGORY_SCHEMA = """
            {
              "type": "array",
              "minItems": %d,
              "maxItems": %d,
              "items": {
                "type": "object",
                "properties": {
                  "subsidyId": {"type": "integer"},
                  "category": {
                    "type": "string",
                    "enum": ["생활안정", "주거자립", "교육", "취업", "금융", "보건의료", "보호돌봄"]
                  }
                },
                "required": ["subsidyId", "category"]
              }
            }
            """;

    private static final String PROMPT_HEADER = """
            아래는 정부 지원금 원본 데이터 %d건이야. 각 subsidyId마다 정확히 하나씩, 다음 7개
            카테고리 중 가장 알맞은 것 하나를 반드시 골라서 총 %d개를 배열로 반환해 (생략 금지):

            - 생활안정: 생계비, 자립수당, 정착금 등 현금성 생활 지원
            - 주거자립: 임대주택, 주거비, 전세자금 등 주거 관련 지원
            - 교육: 장학금, 등록금, 학원비 등 교육 관련 지원
            - 취업: 취업지원, 직업훈련, 구직활동 지원
            - 금융: 대출, 저축, 금융교육
            - 보건의료: 의료비, 건강검진, 심리상담 등 의료/건강 지원
            - 보호돌봄: 상담, 돌봄, 보호 서비스

            원문에 분류 관련 필드(예: mclsfNm, lclsfNm, 서비스분야, 지원유형 등)가 있으면
            그걸 최우선으로 참고해서 판단해. 애매하더라도 가장 가까운 카테고리 하나는 꼭 골라야 해.

            원본 데이터:
            """;

    private final SubsidyRepository subsidyRepository;
    private final SubsidyRawRepository subsidyRawRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryBackfillRunner(SubsidyRepository subsidyRepository,
                                  SubsidyRawRepository subsidyRawRepository,
                                  GeminiClient geminiClient) {
        this.subsidyRepository = subsidyRepository;
        this.subsidyRawRepository = subsidyRawRepository;
        this.geminiClient = geminiClient;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Subsidy> targets = subsidyRepository.findAll();
        System.out.println("카테고리 재정리 대상: " + targets.size() + "건");

        for (int i = 0; i < targets.size(); i += BATCH_SIZE) {
            List<Subsidy> batch = targets.subList(i, Math.min(i + BATCH_SIZE, targets.size()));
            processBatch(batch);
        }
    }

    private void processBatch(List<Subsidy> batch) throws Exception {
        Map<Long, Subsidy> bySubsidyId = batch.stream()
                .collect(Collectors.toMap(Subsidy::getId, s -> s));

        String prompt = PROMPT_HEADER.formatted(batch.size(), batch.size()) + batch.stream()
                .map(s -> {
                    SubsidyRaw raw = subsidyRawRepository.findById(s.getRawId()).orElseThrow();
                    return "subsidyId: " + s.getId() + "\n" + raw.getRawPayload();
                })
                .collect(Collectors.joining("\n---\n"));

        String responseJson = geminiClient.generateJson(prompt, CATEGORY_SCHEMA.formatted(batch.size(), batch.size()));
        List<CategoryResult> results = objectMapper.readValue(responseJson, new TypeReference<List<CategoryResult>>() {});

        if (results.size() != batch.size()) {
            System.out.println("경고: 요청 " + batch.size() + "건, 응답 " + results.size() + "건 — 개수 불일치");
        }

        for (CategoryResult r : results) {
            Subsidy subsidy = bySubsidyId.get(r.subsidyId());
            if (subsidy == null) {
                System.out.println("경고: subsidyId " + r.subsidyId() + " 를 배치에서 못 찾음, 건너뜀");
                continue;
            }
            subsidy.setCategory(r.category());
            subsidyRepository.save(subsidy);
        }

        System.out.println(batch.size() + "건 배치 완료 (" + results.size() + "건 응답받음)");
    }

    private record CategoryResult(Long subsidyId, String category) {}
}