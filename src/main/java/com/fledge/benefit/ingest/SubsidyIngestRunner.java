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

    // "자립준비청년" 문구가 없어도 이 사용자들에게 맞을 만한 일반 청년 정책을 넓히기 위해 추가.
    // 이미 있는 항목은 (source, externalId) 로 걸러지니 다시 돌려도 중복 저장되지 않는다
    private static final List<String> KEYWORDS = List.of(
            "자립준비청년",
            "청년도약계좌",
            "청년내일저축계좌",
            "청년마음건강",
            "청년월세",
            "국민취업지원제도",
            "국민내일배움카드"
    );

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
        for (String keyword : KEYWORDS) {
            System.out.println("=== 키워드: " + keyword + " ===");
            save("welfare", welfareRawClient.fetchList(keyword, 1, 100));
            save("gov24", gov24RawClient.fetchList(keyword, 1, 100));
            save("youthcenter", youthCenterRawClient.fetchList(keyword, 1, 100));
        }
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