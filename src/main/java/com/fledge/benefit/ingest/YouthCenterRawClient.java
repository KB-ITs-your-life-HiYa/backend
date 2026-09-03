//온통청년 - 청년정책 API
package com.fledge.benefit.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class YouthCenterRawClient {

    private static final String BASE_URL = "https://www.youthcenter.go.kr/go/ythip/getPlcy";

    // 기본값을 두는 이유: 이 빈은 조건 없이 항상 생성된다.
    // 값이 없으면 키를 갖지 않은 팀원은 앱 자체를 기동할 수 없다.
    // 실제 호출은 수집 프로필(ingest/parse)에서만 일어난다.
    @Value("${youthcenter.service-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    public List<RawSubsidy> fetchList(String keyword, int pageNum, int pageSize) {
        JsonNode response = restClient.get()
                .uri(BASE_URL + "?apiKeyNm={key}&pageNum={pageNum}&pageSize={pageSize}&pageType=1&rtnType=json&plcyNm={keyword}",
                        apiKey, pageNum, pageSize, keyword)
                .retrieve()
                .body(JsonNode.class);

        List<RawSubsidy> result = new ArrayList<>();
        for (JsonNode item : response.get("result").get("youthPolicyList")) {
            result.add(new RawSubsidy(item.get("plcyNo").asText(), item.toString()));
        }
        return result;
    }
}