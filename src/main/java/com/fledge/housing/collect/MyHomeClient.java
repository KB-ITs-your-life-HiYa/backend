package com.fledge.housing.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 마이홈포털 임대주택 공고 조회.
 *
 * 공공데이터포털 → 국토교통부 마이홈포털 임대주택 공고문 조회 서비스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyHomeClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/HWSPR02";
    private static final String PATH = "/rsdtRcritNtcList";

    // 기본값을 두는 이유: 이 빈은 조건 없이 항상 생성된다.
    // 키가 없는 팀원도 앱은 켤 수 있어야 한다. 실제 호출은 수집을 켰을 때만 일어난다.
    @Value("${myhome.service-key:}")
    private String serviceKey;

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create(BASE_URL);

    /** 한 페이지를 가져온다. 응답 형식이 예상과 다르면 빈 목록을 돌려준다 */
    public MyHomePage fetch(int pageNo, int numOfRows) {
        JsonNode root = restClient.get()
                .uri(builder -> builder
                        .path(PATH)
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        JsonNode body = root == null ? null : root.path("response").path("body");
        if (body == null || body.isMissingNode()) {
            // 키가 잘못됐거나 서비스가 내려가면 OpenAPI_ServiceResponse 형태의 에러가 온다
            log.warn("마이홈포털 응답 형식이 예상과 다릅니다. {}", abbreviate(root));
            return new MyHomePage(List.of(), 0);
        }

        JsonNode itemNode = body.path("item");
        List<MyHomeItem> items = itemNode.isArray()
                ? objectMapper.convertValue(itemNode, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, MyHomeItem.class))
                : List.of();

        return new MyHomePage(items, body.path("totalCount").asInt());
    }

    private String abbreviate(JsonNode node) {
        String s = node == null ? "null" : node.toString();
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    /** 한 페이지 결과. totalCount 로 다음 페이지가 있는지 판단한다 */
    public record MyHomePage(List<MyHomeItem> items, int totalCount) {
    }
}
