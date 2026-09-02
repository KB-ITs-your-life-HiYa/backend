//행정안전부_대한민국 공공서비스(혜택)
package com.fledge.benefit.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class Gov24RawClient {

    private static final String BASE_URL = "https://api.odcloud.kr/api/gov24/v3";

    @Value("${gov24.service-key}")
    private String serviceKey;

    private final RestClient restClient = RestClient.create(BASE_URL);

    public List<RawSubsidy> fetchList(String keyword, int page, int perPage) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/serviceList")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParam("cond[서비스명::LIKE]", keyword)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        List<RawSubsidy> result = new ArrayList<>();
        for (JsonNode item : response.get("data")) {
            result.add(new RawSubsidy(item.get("서비스ID").asText(), item.toString()));
        }
        return result;
    }
}