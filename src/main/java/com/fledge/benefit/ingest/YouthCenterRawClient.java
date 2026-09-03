package com.fledge.benefit.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class YouthCenterRawClient {
    private static final String BASE_URL = "https://www.youthcenter.go.kr/go/ythip/getPlcy";
    @Value("${youthcenter.service-key:}")
    private String apiKey;
    private final RestClient restClient;

    public YouthCenterRawClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        restClient = RestClient.builder().requestFactory(factory).build();
    }

    public List<RawSubsidy> fetchList(String keyword, int pageNum, int pageSize) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("온통청년 인증키 미설정");
        JsonNode response = restClient.get()
                .uri(BASE_URL + "?apiKeyNm={key}&pageNum={pageNum}&pageSize={pageSize}&pageType=1&rtnType=json&plcyNm={keyword}",
                        apiKey, pageNum, pageSize, keyword)
                .retrieve().body(JsonNode.class);
        if (response == null || response.path("resultCode").asInt() != 200
                || !response.path("result").path("youthPolicyList").isArray())
            throw new IllegalStateException("온통청년 응답 형식 또는 인증 오류");
        List<RawSubsidy> result = new ArrayList<>();
        for (JsonNode item : response.path("result").path("youthPolicyList")) {
            if (!item.path("plcyNo").asText("").isBlank())
                result.add(new RawSubsidy(item.path("plcyNo").asText(), item.toString()));
        }
        return result;
    }
}
