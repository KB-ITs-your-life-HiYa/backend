package com.fledge.housing.repository;

import com.fledge.housing.domain.HousingNoticeUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HousingNoticeUnitRepository extends JpaRepository<HousingNoticeUnit, Long> {

    List<HousingNoticeUnit> findByNoticeId(Long noticeId);

    /** 수집할 때 공고별로 전부 지우고 다시 넣는다. 부분 갱신보다 단순하고 단지 수가 적어 비용도 낮다 */
    void deleteByNoticeId(Long noticeId);
}
