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

    @Value("${youthcenter.service-key}")
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