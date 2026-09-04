package com.fledge.benefit.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    // 기본값을 두는 이유: 이 빈은 조건 없이 항상 생성된다.
    // 값이 없으면 키를 갖지 않은 팀원은 앱 자체를 기동할 수 없다.
    // 실제 호출은 수집 프로필(ingest/parse)에서만 일어난다.
    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String model;

    private final RestClient restClient = createClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateJson(String prompt, String responseSchemaJson) throws Exception {
        return generateJson(prompt, responseSchemaJson, 32768);
    }

    public String generateJson(String prompt, String responseSchemaJson, int maxOutputTokens) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }
        JsonNode schema = objectMapper.readTree(responseSchemaJson);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", schema,
                        "temperature", 0,
                        "maxOutputTokens", maxOutputTokens
                )
        );

        JsonNode response = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("candidates").isArray() || response.path("candidates").isEmpty())
            throw new IllegalStateException("Gemini response has no candidates");
        JsonNode candidate = response.path("candidates").get(0);
        String finishReason = candidate.path("finishReason").asText();
        if (!"STOP".equals(finishReason)) {
            log.warn("Gemini 응답이 정상 종료되지 않음: finishReason={}", finishReason);
        }
        JsonNode text = candidate.path("content").path("parts").path(0).path("text");
        if (!text.isTextual() || text.asText().isBlank())
            throw new IllegalStateException("Gemini response has no text");
        return text.asText();
    }

    private static RestClient createClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().baseUrl(BASE_URL).requestFactory(factory).build();
    }
}
