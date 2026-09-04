package com.fledge.benefit.ingest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiClientTest {
    @Test
    void missingApiKeyFailsBeforeMakingARequest() {
        GeminiClient client = new GeminiClient();
        assertThatThrownBy(() -> client.generateJson("prompt", "{\"type\":\"object\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API key");
    }
}
