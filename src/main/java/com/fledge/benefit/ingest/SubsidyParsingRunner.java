package com.fledge.benefit.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.domain.Subsidy;
import com.fledge.benefit.domain.SubsidyBenefit;
import com.fledge.benefit.domain.SubsidyRaw;
import com.fledge.benefit.domain.SubsidyRegion;
import com.fledge.benefit.repository.SubsidyBenefitRepository;
import com.fledge.benefit.repository.SubsidyRawRepository;
import com.fledge.benefit.repository.SubsidyRegionRepository;
import com.fledge.benefit.repository.SubsidyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Profile("parse")
public class SubsidyParsingRunner implements CommandLineRunner {

    private static final int BATCH_SIZE = 5;

    private String buildResponseSchema(int count) {
        return """
            {
              "type": "array",
              "minItems": %d,
              "maxItems": %d,
              "items": {
                "type": "object",
                "properties": {
                  "rawId": {"type": "integer"},
                  "name": {"type": "string"},
                  "summary": {"type": "string"},
                  "orgName": {"type": "string"},
                  "category": {"type": "string"},
                  "applyMethod": {"type": "string"},
                  "applyDeadlineRaw": {"type": "string"},
                  "detailUrl": {"type": "string"},
                  "minAge": {"type": "integer", "nullable": true},
                  "maxAge": {"type": "integer", "nullable": true},
                  "incomePctMax": {"type": "integer", "nullable": true},
                  "incomeAmtMin": {"type": "integer", "nullable": true},
                  "incomeAmtMax": {"type": "integer", "nullable": true},
                  "protectionStatusRequired": {"type": "string", "nullable": true},
                  "minYearsAfterEnd": {"type": "number", "nullable": true},
                  "maxYearsAfterEnd": {"type": "number", "nullable": true},
                  "targetHousehold": {
                    "type": "array", 
                    "items": {
                        "type": "string",
                        "enum": ["SINGLE_PARENT", "MULTICULTURAL", "DISABILITY", "MULTI_CHILD", "SEVERE_ILLNESS", "NORTH_KOREAN_DEFECTOR", "GRANDPARENT_FAMILY"]
                    }
                  },
                  "benefits": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {
                        "benefitName": {"type": "string"},
                        "amountKrw": {"type": "integer", "nullable": true},
                        "cycle": {"type": "string", "nullable": true}
                      },
                      "required": ["benefitName"]
                    }
                  },
                  "regions": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {
                        "sidoCode": {"type": "string"},
                        "sigunguCode": {"type": "string", "nullable": true}
                      },
                      "required": ["sidoCode"]
                    }
                  }
                },
                "required": ["rawId", "name"]
              }
            }
            """.formatted(count, count);
    }

    private static final String PROMPT_HEADER = """
            너는 한국 정부 지원금 데이터를 정리하는 파서야. 아래 원본 데이터는 총 %d건이고,
            각 rawId마다 정확히 하나씩 총 %d개의 결과를 배열로 반환해야 해. 절대 요약하거나 합치지 마.
            
            각 rawId별로 다음 필드를 원문에서 뽑아서 JSON으로 만들어:

            - name: 지원금명
            - summary: 한두 문장 요약
            - orgName: 소관/운영 기관명
            - category: 지원 분야 (예: 주거, 생활지원, 취업, 금융 등)
            - applyMethod: 신청방법
            - applyDeadlineRaw: 신청기한 원문 텍스트
            - detailUrl: 상세페이지 링크 (있으면)
            - minAge / maxAge: 나이 제한 (없으면 null)
            - incomePctMax: "기준중위소득 N%% 이하" 형태 조건의 N (없으면 null)
            - incomeAmtMin / incomeAmtMax: 소득 조건이 절대금액(원)으로 명시된 경우 (없으면 null)
            - protectionStatusRequired: "보호종료" 또는 "보호중" 조건이 명시돼 있으면, 없으면 null
            - minYearsAfterEnd / maxYearsAfterEnd: "보호종료 후 N년 이내" 같은 조건의 N (없으면 null)
            - targetHousehold: 다음 중 해당하는 것만 배열로: SINGLE_PARENT(한부모), MULTICULTURAL(다문화),
              DISABILITY(장애인), SEVERE_ILLNESS(중증질환), NORTH_KOREAN_DEFECTOR(북한이탈주민),
              GRANDPARENT_FAMILY(조손가정), MULTI_CHILD(다자녀). 소득 수준이나 "자립준비청년" 자체는
              여기 넣지 마 (다른 필드/전제 조건에서 이미 다뤄짐). 해당 없으면 빈 배열.
            - benefits: 지원내용에 금액이 여러 항목으로 나뉘어 있으면 각각 {benefitName, amountKrw(원 단위 숫자), cycle(월지급/일시금/분할지급/연지급 중 하나)}로 분리. 금액 정보가 없으면 빈 배열.
            - regions: 특정 지역 대상이면 {sidoCode, sigunguCode} 형태로. 전국 대상이면 빈 배열.

            확실하지 않은 값은 추측해서 채우지 말고 null 또는 빈 배열로 남겨.

            원본 데이터:
            """;

    private final SubsidyRawRepository subsidyRawRepository;
    private final SubsidyRepository subsidyRepository;
    private final SubsidyBenefitRepository subsidyBenefitRepository;
    private final SubsidyRegionRepository subsidyRegionRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubsidyParsingRunner(SubsidyRawRepository subsidyRawRepository,
                                SubsidyRepository subsidyRepository,
                                SubsidyBenefitRepository subsidyBenefitRepository,
                                SubsidyRegionRepository subsidyRegionRepository,
                                GeminiClient geminiClient) {
        this.subsidyRawRepository = subsidyRawRepository;
        this.subsidyRepository = subsidyRepository;
        this.subsidyBenefitRepository = subsidyBenefitRepository;
        this.subsidyRegionRepository = subsidyRegionRepository;
        this.geminiClient = geminiClient;
    }

    @Override
    public void run(String... args) throws Exception {
        List<SubsidyRaw> targets = subsidyRawRepository.findAll().stream()
                .filter(raw -> !subsidyRepository.existsByRawId(raw.getId()))
                .toList();

        System.out.println("파싱 대상: " + targets.size() + "건");

        for (int i = 0; i < targets.size(); i += BATCH_SIZE) {
            List<SubsidyRaw> batch = targets.subList(i, Math.min(i + BATCH_SIZE, targets.size()));
            processBatch(batch);
        }
    }

    private void processBatch(List<SubsidyRaw> batch) throws Exception {
        Map<Long, SubsidyRaw> byRawId = batch.stream()
                .collect(Collectors.toMap(SubsidyRaw::getId, raw -> raw));

        String prompt = PROMPT_HEADER.formatted(batch.size(), batch.size()) + batch.stream()
                .map(raw -> "rawId: " + raw.getId() + ", source: " + raw.getSource() + "\n" + raw.getRawPayload())
                .collect(Collectors.joining("\n---\n"));

        String responseJson = geminiClient.generateJson(prompt, buildResponseSchema(batch.size()));
        List<ParsedSubsidy> parsedList = objectMapper.readValue(responseJson, new TypeReference<List<ParsedSubsidy>>() {});

        if (parsedList.size() != batch.size()) {
            System.out.println("경고: 요청 " + batch.size() + "건, 응답 " + parsedList.size() + "건 — 개수 불일치");
        }

        for (ParsedSubsidy parsed : parsedList) {
            SubsidyRaw raw = byRawId.get(parsed.rawId());
            if (raw == null) {
                System.out.println("경고: rawId " + parsed.rawId() + " 를 원본에서 못 찾음, 건너뜀");
                continue;
            }
            saveSubsidy(raw, parsed);
        }

        System.out.println(batch.size() + "건 배치 처리 완료 (" + parsedList.size() + "건 응답받음)");
    }

    private void saveSubsidy(SubsidyRaw raw, ParsedSubsidy parsed) {
        Subsidy subsidy = new Subsidy(raw.getSource(), raw.getExternalId(), raw.getId(), parsed.name());
        subsidy.setSummary(parsed.summary());
        subsidy.setOrgName(parsed.orgName());
        subsidy.setCategory(parsed.category());
        subsidy.setApplyMethod(parsed.applyMethod());
        subsidy.setApplyDeadlineRaw(parsed.applyDeadlineRaw());
        subsidy.setDetailUrl(parsed.detailUrl());
        subsidy.setMinAge(parsed.minAge());
        subsidy.setMaxAge(parsed.maxAge());
        subsidy.setIncomePctMax(parsed.incomePctMax());
        subsidy.setIncomeAmtMin(parsed.incomeAmtMin());
        subsidy.setIncomeAmtMax(parsed.incomeAmtMax());
        subsidy.setProtectionStatusRequired(parsed.protectionStatusRequired());
        subsidy.setMinYearsAfterEnd(parsed.minYearsAfterEnd());
        subsidy.setMaxYearsAfterEnd(parsed.maxYearsAfterEnd());
        subsidy.setTargetHousehold(parsed.targetHousehold());

        subsidyRepository.save(subsidy);

        if (parsed.benefits() != null) {
            for (ParsedSubsidy.ParsedBenefit b : parsed.benefits()) {
                subsidyBenefitRepository.save(new SubsidyBenefit(subsidy, b.benefitName(), b.amountKrw(), b.cycle()));
            }
        }

        if (parsed.regions() != null) {
            for (ParsedSubsidy.ParsedRegion r : parsed.regions()) {
                subsidyRegionRepository.save(new SubsidyRegion(subsidy, r.sidoCode(), r.sigunguCode()));
            }
        }
    }
}