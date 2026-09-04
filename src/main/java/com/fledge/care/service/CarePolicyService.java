package com.fledge.care.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.ingest.YouthCenterRawClient;
import com.fledge.care.dto.CareDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/** 외부 HTTP 호출 동안 회원 잠금이나 DB 트랜잭션을 유지하지 않는다. */
@Service
@RequiredArgsConstructor
public class CarePolicyService {
    private final CareService care;
    private final YouthCenterRawClient youth;
    private final ObjectMapper mapper;

    public Summary load(Long memberId, Long signalId, Long responseId) {
        PolicyContext context = care.policyContext(memberId, signalId, responseId);
        if (context.ready()) return care.summary(memberId);
        Policies result;
        try {
            boolean employment = "INCOME_MISSING".equals(context.signalType());
            List<String> keywords = employment ? List.of("일경험", "국민취업지원") : List.of("생활비", "햇살론");
            Map<String, PolicyCard> cards = new LinkedHashMap<>();
            for (String keyword : keywords) {
                for (var raw : youth.fetchList(keyword, 1, 100)) {
                    JsonNode item = mapper.readTree(raw.rawPayload());
                    PolicyCard card = toCard(item, employment, context.asOf());
                    if (card != null) cards.putIfAbsent(card.id(), card);
                }
            }
            // 같은 API 응답에는 항상 같은 순서. 전국 정책을 먼저 안내하고 원문 기관도 함께 표시한다.
            Set<String> supportSeen = new HashSet<>();
            Set<String> nameSeen = new HashSet<>();
            List<PolicyCard> selected = cards.values().stream()
                    .sorted(Comparator.comparing((PolicyCard c) -> !isNational(c.organization()))
                            .thenComparing(PolicyCard::id, Comparator.reverseOrder()))
                    .filter(c -> nameSeen.add(c.name().replaceAll("\\s+", "")))
                    // 기관별로 중복 등록된 같은 지원 내용을 두 카드로 보여주지 않는다.
                    .filter(c -> c.support().length() < 20 || supportSeen.add(c.support().replaceAll("\\s+", "")))
                    .limit(2).toList();
            result = new Policies(selected.isEmpty() ? "EMPTY" : "READY", selected);
        } catch (Exception e) {
            // RestClient 예외에는 인증키가 든 요청 URL이 포함될 수 있어 기록/응답에 노출하지 않는다.
            result = new Policies("ERROR", List.of());
        }
        return care.savePolicies(memberId, signalId, responseId, result);
    }

    static PolicyCard toCard(JsonNode item, boolean employment, LocalDate asOf) {
        String id = text(item, "plcyNo");
        String name = text(item, "plcyNm");
        if (!id.matches("[0-9]+") || name.isBlank()) return null;
        String period = text(item, "aplyYmd");
        if (expired(period, asOf)) return null;
        String support = text(item, "plcySprtCn");
        if (support.isBlank()) support = text(item, "plcyExplnCn");
        return new PolicyCard(id, employment ? "EMPLOYMENT" : "FINANCE", name,
                support.isBlank() ? "지원 내용은 정책 상세에서 확인해 주세요." : support,
                period.isBlank() ? "신청 기간은 정책 상세에서 확인해 주세요." : period,
                text(item, "sprvsnInstCdNm"),
                "https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/" + id);
    }

    private static boolean isNational(String organization) {
        return List.of("고용노동부", "보건복지부", "금융위원회", "서민금융진흥원", "교육부").contains(organization);
    }

    private static String text(JsonNode item, String field) {
        return item.path(field).asText("").replaceAll("<[^>]*>", " ").trim();
    }

    private static boolean expired(String period, LocalDate asOf) {
        var matcher = Pattern.compile("(?<![0-9])([0-9]{8})\\s*~\\s*([0-9]{8})(?![0-9])").matcher(period);
        LocalDate latest = null;
        while (matcher.find()) {
            try {
                LocalDate end = LocalDate.parse(matcher.group(2), DateTimeFormatter.BASIC_ISO_DATE);
                if (latest == null || end.isAfter(latest)) latest = end;
            } catch (java.time.DateTimeException ignored) { return false; }
        }
        return latest != null && latest.isBefore(asOf);
    }
}
