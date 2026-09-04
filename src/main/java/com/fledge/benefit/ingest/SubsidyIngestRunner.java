//1회성 실행 트리거
package com.fledge.benefit.ingest;

import com.fledge.benefit.domain.SubsidyRaw;
import com.fledge.benefit.repository.SubsidyRawRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("ingest")
public class SubsidyIngestRunner implements CommandLineRunner {

    private static final String KEYWORD = "자립준비청년";

    private final WelfareRawClient welfareRawClient;
    private final Gov24RawClient gov24RawClient;
    private final YouthCenterRawClient youthCenterRawClient;
    private final SubsidyRawRepository subsidyRawRepository;

    public SubsidyIngestRunner(WelfareRawClient welfareRawClient,
                               Gov24RawClient gov24RawClient,
                               YouthCenterRawClient youthCenterRawClient,
                               SubsidyRawRepository subsidyRawRepository) {
        this.welfareRawClient = welfareRawClient;
        this.gov24RawClient = gov24RawClient;
        this.youthCenterRawClient = youthCenterRawClient;
        this.subsidyRawRepository = subsidyRawRepository;
    }

    @Override
    public void run(String... args) {
        save("welfare", welfareRawClient.fetchList(KEYWORD, 1, 100));
        save("gov24", gov24RawClient.fetchList(KEYWORD, 1, 100));
        save("youthcenter", youthCenterRawClient.fetchList(KEYWORD, 1, 100));
    }

    private void save(String source, List<RawSubsidy> records) {
        int savedCount = 0;
        for (RawSubsidy record : records) {
            boolean alreadyExists = subsidyRawRepository
                    .findBySourceAndExternalId(source, record.externalId())
                    .isPresent();

            if (alreadyExists) {
                continue;
            }

            subsidyRawRepository.save(
                    new SubsidyRaw(source, record.externalId(), record.rawPayload(), LocalDateTime.now()));
            savedCount++;
        }
        System.out.println(source + ": 총 " + records.size() + "건 중 " + savedCount + "건 신규 저장");
    }
}