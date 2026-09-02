package com.fledge.benefit.ingest;

import com.fledge.benefit.domain.SubsidyRaw;
import com.fledge.benefit.repository.SubsidyRawRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("gemini-test")
public class GeminiTestRunner implements CommandLineRunner {

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "name": {"type": "string"},
                "summary": {"type": "string"},
                "minAge": {"type": "integer", "nullable": true},
                "maxAge": {"type": "integer", "nullable": true}
              },
              "required": ["name", "summary"]
            }
            """;

    private final SubsidyRawRepository subsidyRawRepository;
    private final GeminiClient geminiClient;

    public GeminiTestRunner(SubsidyRawRepository subsidyRawRepository, GeminiClient geminiClient) {
        this.subsidyRawRepository = subsidyRawRepository;
        this.geminiClient = geminiClient;
    }

    @Override
    public void run(String... args) throws Exception {
        SubsidyRaw raw = subsidyRawRepository.findAll().get(0);

        String prompt = """
                다음은 정부 지원금 원본 데이터야. name(지원금명), summary(한 줄 요약),
                minAge(최소 나이, 없으면 null), maxAge(최대 나이, 없으면 null)를 뽑아서 JSON으로 반환해줘.

                원본 데이터:
                %s
                """.formatted(raw.getRawPayload());

        String result = geminiClient.generateJson(prompt, SCHEMA);
        System.out.println("=== Gemini 응답 ===");
        System.out.println(result);
    }
}