package com.fledge.care.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.ingest.Gov24RawClient;
import com.fledge.benefit.ingest.RawSubsidy;
import com.fledge.benefit.ingest.WelfareRawClient;
import com.fledge.benefit.ingest.YouthCenterRawClient;
import com.fledge.housing.collect.MyHomeClient;
import com.fledge.housing.collect.MyHomeClient.MyHomePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1회성 실행 트리거: 지원금(welfare/gov24/youthcenter)·독립지원(myhome) 공공 API를
 * 이미 쓰고 있는 클라이언트로 그대로 호출해서, RAG가 읽을 수 있는 JSON으로 docs/rag 에 저장한다.
 *
 * 【실행 방법】
 *   SPRING_PROFILES_ACTIVE=local,rag-export ./gradlew bootRun
 *   (PowerShell) $env:SPRING_PROFILES_ACTIVE="local,rag-export"; ./gradlew bootRun
 *
 * application-secret.properties 에 welfare.service-key / gov24.service-key /
 * youthcenter.service-key / myhome.service-key 가 채워져 있어야 실제로 값이 나온다.
 * 키가 없거나 API 호출이 실패해도 그 소스만 건너뛰고 나머지는 계속 진행한다 — 다른 ingest/parse
 * 러너들과 같은 방침이다.
 *
 * SubsidyIngestRunner(ingest 프로필)와 달리 DB에 저장하지 않는다. 이 데이터는 회원 매칭용이
 * 아니라 챗봇이 답변 근거로 참고하는 "문서"이기 때문에, RagKnowledgeBase 가 그대로 읽을 수 있는
 * [{"title":..., "content":..., "sourceUrl":...}] 배열 형태로 파일에 바로 쓴다.
 */
@Component
@Profile("rag-export")
@RequiredArgsConstructor
@Slf4j
public class RagDataExportRunner implements CommandLineRunner {

    private static final String KEYWORD = "자립준비청년";
    // 마이홈포털은 키워드 검색이 없어 전체를 받은 뒤 공고명으로 거른다 (TargetTypeResolver 와 같은 기준)
    private static final String[] HOUSING_YOUTH_KEYWORDS = {"자립준비", "보호종료", "보호대상아동", "청년", "대학생"};
    private static final int HOUSING_PAGE_SIZE = 300;
    private static final int HOUSING_MAX_PAGES = 5;

    @Value("${rag.docs-path:docs/rag}")
    private String docsPath;

    private final WelfareRawClient welfare;
    private final Gov24RawClient gov24;
    private final YouthCenterRawClient youthCenter;
    private final MyHomeClient myHome;
    private final ObjectMapper mapper;

    @Override
    public void run(String... args) {
        exportWelfare();
        exportGov24();
        exportYouthCenter();
        exportMyHome();
        log.info("RAG 데이터 내보내기 끝. docs/rag 를 확인하세요 (폴더: {})", Path.of(docsPath).toAbsolutePath());
    }

    private void exportWelfare() {
        try {
            List<RawSubsidy> raws = welfare.fetchList(KEYWORD, 1, 100);
            List<Map<String, String>> docs = new ArrayList<>();
            for (RawSubsidy raw : raws) {
                Map<String, String> fields = mapper.readValue(raw.rawPayload(), new TypeReference<Map<String, String>>() {});
                String title = firstNonBlank(fields, "servNm", "title", "name");
                if (title == null) title = "복지 서비스 " + raw.externalId();
                String content = toContentLines(fields);
                if (content.isBlank()) continue;
                docs.add(chunkMap(title, content, null));
            }
            writeJson("benefits/benefits_welfare.json", docs);
        } catch (Exception e) {
            log.warn("복지로(welfare) 데이터 내보내기 실패: {}", e.getMessage());
        }
    }

    private void exportGov24() {
        try {
            List<RawSubsidy> raws = gov24.fetchList(KEYWORD, 1, 100);
            List<Map<String, String>> docs = new ArrayList<>();
            for (RawSubsidy raw : raws) {
                JsonNode item = mapper.readTree(raw.rawPayload());
                String title = text(item, "서비스명");
                if (title.isBlank()) title = "공공서비스 " + raw.externalId();
                String url = text(item, "상세조회URL");
                String content = toContentLines(item);
                if (content.isBlank()) continue;
                docs.add(chunkMap(title, content, url.isBlank() ? null : url));
            }
            writeJson("benefits/benefits_gov24.json", docs);
        } catch (Exception e) {
            log.warn("정부24(gov24) 데이터 내보내기 실패: {}", e.getMessage());
        }
    }

    private void exportYouthCenter() {
        try {
            List<RawSubsidy> raws = youthCenter.fetchList(KEYWORD, 1, 100);
            List<Map<String, String>> docs = new ArrayList<>();
            for (RawSubsidy raw : raws) {
                JsonNode item = mapper.readTree(raw.rawPayload());
                String title = text(item, "plcyNm");
                if (title.isBlank()) continue;
                String support = text(item, "plcySprtCn");
                if (support.isBlank()) support = text(item, "plcyExplnCn");
                String period = text(item, "aplyYmd");
                String org = text(item, "sprvsnInstCdNm");
                StringBuilder content = new StringBuilder();
                if (!support.isBlank()) content.append("지원 내용: ").append(support).append("\n");
                if (!period.isBlank()) content.append("신청 기간: ").append(period).append("\n");
                if (!org.isBlank()) content.append("담당 기관: ").append(org).append("\n");
                if (content.isEmpty()) continue;
                String url = "https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/" + raw.externalId();
                docs.add(chunkMap(title, content.toString().strip(), url));
            }
            writeJson("benefits/benefits_youthcenter.json", docs);
        } catch (Exception e) {
            log.warn("온통청년(youthcenter) 데이터 내보내기 실패: {}", e.getMessage());
        }
    }

    private void exportMyHome() {
        try {
            List<Map<String, String>> docs = new ArrayList<>();
            for (int pageNo = 1; pageNo <= HOUSING_MAX_PAGES; pageNo++) {
                MyHomePage page = myHome.fetch(pageNo, HOUSING_PAGE_SIZE);
                if (page.items().isEmpty()) break;
                for (var item : page.items()) {
                    String name = item.pblancNm();
                    if (name == null || !containsAny(name, HOUSING_YOUTH_KEYWORDS)) continue; // 청년 관련 공고만
                    StringBuilder content = new StringBuilder();
                    appendIfPresent(content, "공급 기관", item.suplyInsttNm());
                    appendIfPresent(content, "주택 유형", item.houseTyNm());
                    appendIfPresent(content, "공급 유형", item.suplyTyNm());
                    appendIfPresent(content, "접수 기간", joinPeriod(item.beginDe(), item.endDe()));
                    appendIfPresent(content, "단지", item.hsmpNm());
                    appendIfPresent(content, "지역", joinRegion(item.brtcNm(), item.signguNm()));
                    appendIfPresent(content, "임대 보증금", item.rentGtn());
                    appendIfPresent(content, "월 임대료", item.mtRntchrg());
                    appendIfPresent(content, "문의", item.refrnc());
                    if (content.isEmpty()) continue;
                    String url = firstNonBlankValue(item.url(), item.pcUrl());
                    docs.add(chunkMap(name, content.toString().strip(), url));
                }
            }
            writeJson("housing/housing_myhome.json", docs);
        } catch (Exception e) {
            log.warn("마이홈포털(myhome) 데이터 내보내기 실패: {}", e.getMessage());
        }
    }

    // ===== 공통 유틸 =====

    private static Map<String, String> chunkMap(String title, String content, String sourceUrl) {
        Map<String, String> doc = new LinkedHashMap<>();
        doc.put("title", title);
        doc.put("content", content);
        if (sourceUrl != null && !sourceUrl.isBlank()) doc.put("sourceUrl", sourceUrl);
        return doc;
    }

    private void writeJson(String relativePath, List<Map<String, String>> docs) throws IOException {
        Path file = Path.of(docsPath, relativePath);
        Files.createDirectories(file.getParent());
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(docs);
        Files.writeString(file, json, StandardCharsets.UTF_8);
        log.info("RAG 문서 저장: {} ({}건)", file.toAbsolutePath(), docs.size());
    }

    private static String firstNonBlank(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            String value = fields.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String firstNonBlankValue(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    /** Map을 "키: 값" 줄로 펼친다. 값이 비어 있거나 원본 id류 필드는 뺀다 */
    private static String toContentLines(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        fields.forEach((key, value) -> {
            if (value == null || value.isBlank()) return;
            if (key.toLowerCase().endsWith("id")) return;
            sb.append(key).append(": ").append(value.strip()).append("\n");
        });
        return sb.toString().strip();
    }

    /** JsonNode(object) 버전. gov24 응답처럼 필드 이름이 한글일 때 그대로 쓴다 */
    private static String toContentLines(JsonNode item) {
        StringBuilder sb = new StringBuilder();
        item.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            String value = entry.getValue().asText("");
            if (value.isBlank() || key.contains("ID") || key.contains("URL")) return;
            sb.append(key).append(": ").append(value.strip()).append("\n");
        });
        return sb.toString().strip();
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("").strip();
    }

    private static void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) return;
        sb.append(label).append(": ").append(value.strip()).append("\n");
    }

    private static String joinPeriod(String begin, String end) {
        if ((begin == null || begin.isBlank()) && (end == null || end.isBlank())) return null;
        return (begin == null ? "" : begin) + " ~ " + (end == null ? "" : end);
    }

    private static String joinRegion(String si, String gu) {
        String a = si == null ? "" : si.strip();
        String b = gu == null ? "" : gu.strip();
        String joined = (a + " " + b).strip();
        return joined.isBlank() ? null : joined;
    }

    private static boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
