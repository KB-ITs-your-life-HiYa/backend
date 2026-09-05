package com.fledge.housing.repository;

import com.fledge.housing.domain.HousingNoticeUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HousingNoticeUnitRepository extends JpaRepository<HousingNoticeUnit, Long> {
    List<HousingNoticeUnit> findByNoticeId(Long noticeId);

    /** 수집할 때 공고별로 전부 지우고 다시 넣는다. 부분 갱신보다 단순하고 단지 수가 적어 비용도 낮다 */
    void deleteByNoticeId(Long noticeId);

    // 그 시/도에 단지가 있는 공고 id 목록.
    // brtc_nm 은 이름 문자열이라, 코드를 이름으로 바꾼 뒤 넘겨야 한다 (Sido 로 조회).
    @Query("select distinct u.noticeId from HousingNoticeUnit u where u.brtcNm = :regionName")
    List<Long> findDistinctNoticeIdByRegionName(String regionName);
}
