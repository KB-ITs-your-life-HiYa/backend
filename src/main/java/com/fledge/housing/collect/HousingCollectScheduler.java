package com.fledge.housing.collect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공고 수집 스케줄러.
 *
 * 【로컬에서 꺼두는 이유】
 * 5명이 각자 앱을 띄울 때마다 공공 API 를 호출하면 안 된다.
 * 로컬은 기본으로 꺼두고, 필요하면 POST /housing/admin/collect 로 한 번 돌린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.housing.collect.enabled", havingValue = "true")
public class HousingCollectScheduler {

    private final HousingCollector collector;

    /**
     * 매일 새벽 4시.
     * 공고는 업무시간에 갱신되므로 밤에 한 번이면 충분하고, 사용자가 없는 시간이라 안전하다.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void collect() {
        log.info("공고 수집 시작 (스케줄)");
        collector.collect();
    }
}
