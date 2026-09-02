package com.fledge.benefit.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final RestClient restClient = RestClient.create(BASE_URL);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateJson(String prompt, String responseSchemaJson) throws Exception {
        JsonNode schema = objectMapper.readTree(responseSchemaJson);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", schema,
                        "temperature", 0,
                        "maxOutputTokens", 32768
                )
        );

        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        JsonNode candidate = response.get("candidates").get(0);
        String finishReason = candidate.get("finishReason").asText();
        if (!"STOP".equals(finishReason)) {
            System.out.println("경고: finishReason = " + finishReason + " (STOP이 아니면 응답이 잘렸을 수 있음)");
        }

        return response.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
    }
}