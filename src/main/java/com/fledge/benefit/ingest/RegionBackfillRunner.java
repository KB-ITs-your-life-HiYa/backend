package com.fledge.benefit.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.domain.Subsidy;
import com.fledge.benefit.domain.SubsidyRaw;
import com.fledge.benefit.domain.SubsidyRegion;
import com.fledge.benefit.repository.SubsidyRawRepository;
import com.fledge.benefit.repository.SubsidyRegionRepository;
import com.fledge.benefit.repository.SubsidyRepository;
import com.fledge.region.domain.Sido;
import com.fledge.region.repository.SidoRepository;
import com.fledge.region.repository.SigunguRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 지원금 55건 중 subsidy_region 이 하나도 없는 건(전국 대상인지, 지역 파싱이 안 된 건지
// 구분이 안 되던 것들)을 원문에서 다시 읽어 지역을 채운다. 전국 대상이면 그대로 비워둔다
// (subsidy_region 이 없으면 매칭에서 지역 조건을 안 거는 게 기존 설계다).
@Component
@Profile("backfill-region")
public class RegionBackfillRunner implements CommandLineRunner {

    private static final int BATCH_SIZE = 10;

    private static final String SIDO_NAMES = String.join(", ",
            "서울특별시", "전남광주통합특별시", "부산광역시", "대구광역시", "인천광역시", "대전광역시",
            "울산광역시", "세종특별자치시", "경기도", "강원특별자치도", "충청북도", "충청남도",
            "전북특별자치도", "경상북도", "경상남도", "제주특별자치도");

    private static final String REGION_SCHEMA = """
            {
              "type": "array",
              "minItems": %d,
              "maxItems": %d,
              "items": {
                "type": "object",
                "properties": {
                  "subsidyId": {"type": "integer"},
                  "national": {"type": "boolean"},
                  "regions": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {
                        "sido": {"type": "string"},
                        "sigungu": {"type": "string"}
                      },
                      "required": ["sido"]
                    }
                  }
                },
                "required": ["subsidyId", "national", "regions"]
              }
            }
            """;

    private static final String PROMPT_HEADER = """
            아래는 정부 지원금 원본 데이터 %d건이야. 각 subsidyId마다 정확히 하나씩,
            이 지원금이 전국 대상인지 특정 지역 대상인지 판단해서 총 %d개를 배열로 반환해 (생략 금지).

            판단 기준:
            - 원문에 특정 시/도·시/군/구 이름이 명시돼 있거나(예: 지원금 이름에 "부산", "서산시"가
              들어있음), 소관기관·주관기관이 특정 지자체거나, zipCd/우편번호가 특정 지역 것만
              나열돼 있으면 그 지역이 대상이야. 이때 national=false, regions 에 해당하는
              시/도·시/군/구를 전부 적어.
            - 시/도 전체가 대상이면 sigungu 는 생략해. 특정 시/군/구만 대상이면 sigungu 도 적어.
            - 여러 시/군/구가 대상이면 regions 배열에 여러 개를 넣어.
            - 원문에 지역을 제한하는 단서가 전혀 없으면(중앙부처 사업, "전국" 명시 등) national=true,
              regions 는 빈 배열로 둬.

            sido 값은 반드시 다음 16개 중 하나의 정확한 표기를 그대로 써야 해(2026년 개편 기준,
            예: '광주광역시'가 아니라 '전남광주통합특별시', '강원도'가 아니라 '강원특별자치도'):
            %s

            원본 데이터:
            """;

    private final SubsidyRepository subsidyRepository;
    private final SubsidyRawRepository subsidyRawRepository;
    private final SubsidyRegionRepository subsidyRegionRepository;
    private final SidoRepository sidoRepository;
    private final SigunguRepository sigunguRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegionBackfillRunner(SubsidyRepository subsidyRepository,
                                 SubsidyRawRepository subsidyRawRepository,
                                 SubsidyRegionRepository subsidyRegionRepository,
                                 SidoRepository sidoRepository,
                                 SigunguRepository sigunguRepository,
                                 GeminiClient geminiClient) {
        this.subsidyRepository = subsidyRepository;
        this.subsidyRawRepository = subsidyRawRepository;
        this.subsidyRegionRepository = subsidyRegionRepository;
        this.sidoRepository = sidoRepository;
        this.sigunguRepository = sigunguRepository;
        this.geminiClient = geminiClient;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Subsidy> targets = subsidyRepository.findAll().stream()
                .filter(s -> subsidyRegionRepository.findBySubsidy_Id(s.getId()).isEmpty())
                .toList();
        System.out.println("지역 백필 대상: " + targets.size() + "건");

        for (int i = 0; i < targets.size(); i += BATCH_SIZE) {
            List<Subsidy> batch = targets.subList(i, Math.min(i + BATCH_SIZE, targets.size()));
            processBatch(batch);
        }
    }

    private void processBatch(List<Subsidy> batch) throws Exception {
        Map<Long, Subsidy> bySubsidyId = batch.stream()
                .collect(Collectors.toMap(Subsidy::getId, s -> s));

        String prompt = PROMPT_HEADER.formatted(batch.size(), batch.size(), SIDO_NAMES) + batch.stream()
                .map(s -> {
                    SubsidyRaw raw = subsidyRawRepository.findById(s.getRawId()).orElseThrow();
                    return "subsidyId: " + s.getId() + "\n" + raw.getRawPayload();
                })
                .collect(Collectors.joining("\n---\n"));

        String responseJson = geminiClient.generateJson(prompt, REGION_SCHEMA.formatted(batch.size(), batch.size()));
        List<RegionResult> results = objectMapper.readValue(responseJson, new TypeReference<List<RegionResult>>() {});

        if (results.size() != batch.size()) {
            System.out.println("경고: 요청 " + batch.size() + "건, 응답 " + results.size() + "건 — 개수 불일치");
        }

        for (RegionResult r : results) {
            Subsidy subsidy = bySubsidyId.get(r.subsidyId());
            if (subsidy == null) {
                System.out.println("경고: subsidyId " + r.subsidyId() + " 를 배치에서 못 찾음, 건너뜀");
                continue;
            }
            if (r.national() || r.regions() == null || r.regions().isEmpty()) {
                System.out.println(subsidy.getId() + " " + subsidy.getName() + " -> 전국");
                continue;
            }
            for (RegionEntry entry : r.regions()) {
                saveRegion(subsidy, entry);
            }
        }

        System.out.println(batch.size() + "건 배치 완료 (" + results.size() + "건 응답받음)");
    }

    private void saveRegion(Subsidy subsidy, RegionEntry entry) {
        Sido sido = sidoRepository.findByName(entry.sido()).orElse(null);
        if (sido == null) {
            System.out.println("경고: " + subsidy.getId() + " " + subsidy.getName()
                    + " -> 알 수 없는 시/도명 '" + entry.sido() + "', 건너뜀");
            return;
        }
        String sigunguCode = null;
        if (entry.sigungu() != null && !entry.sigungu().isBlank()) {
            sigunguCode = sigunguRepository.findBySidoCodeAndName(sido.getCode(), entry.sigungu())
                    .map(sg -> sg.getCode())
                    .orElse(null);
            if (sigunguCode == null) {
                System.out.println("경고: " + subsidy.getId() + " " + subsidy.getName() + " -> '"
                        + entry.sido() + " " + entry.sigungu() + "' 시/군/구를 못 찾음, 시/도 전체로 저장");
            }
        }
        subsidyRegionRepository.save(new SubsidyRegion(subsidy, sido.getCode(), sigunguCode));
        System.out.println(subsidy.getId() + " " + subsidy.getName() + " -> " + sido.getName()
                + (sigunguCode != null ? " " + entry.sigungu() : ""));
    }

    private record RegionResult(Long subsidyId, boolean national, List<RegionEntry> regions) {}

    private record RegionEntry(String sido, String sigungu) {}
}
