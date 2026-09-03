package com.fledge.housing.collect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 마이홈포털 공고 수집.
 *
 * 페이지를 돌며 가져오고, 저장은 HousingNoticeWriter 에 맡긴다.
 * 페이지 하나가 실패해도 나머지는 계속 진행한다. 실패는 다음 실행에서 다시 받는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HousingCollector {

    /** 실측 319행이라 한 번에 다 받는다. 늘어나면 다음 페이지를 이어서 받는다 */
    private static final int PAGE_SIZE = 500;
    /** 무한 루프 방지 */
    private static final int MAX_PAGES = 20;

    private final MyHomeClient myHomeClient;
    private final HousingNoticeWriter writer;

    public CollectResult collect() {
        int notices = 0;
        int units = 0;
        int failedPages = 0;

        for (int pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
            MyHomeClient.MyHomePage page;
            try {
                page = myHomeClient.fetch(pageNo, PAGE_SIZE);
            } catch (Exception e) {
                log.warn("공고 조회 실패 - {}페이지", pageNo, e);
                failedPages++;
                continue;
            }

            if (page.items().isEmpty()) {
                break;
            }

            try {
                HousingNoticeWriter.Saved saved = writer.savePage(page.items());
                notices += saved.notices();
                units += saved.units();
            } catch (Exception e) {
                log.warn("공고 저장 실패 - {}페이지", pageNo, e);
                failedPages++;
            }

            if ((long) pageNo * PAGE_SIZE >= page.totalCount()) {
                break;
            }
        }

        int superseded = writer.markSuperseded();

        log.info("공고 수집 완료 - 공고 {}건, 단지 {}건, 대체됨 {}건, 실패 {}페이지",
                notices, units, superseded, failedPages);
        return new CollectResult(notices, units, superseded, failedPages);
    }

    public record CollectResult(int notices, int units, int superseded, int failedPages) {
    }
}
