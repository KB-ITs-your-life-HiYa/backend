package com.fledge.housing.repository;

import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HousingNoticeRepository extends JpaRepository<HousingNotice, Long> {

    Optional<HousingNotice> findByPblancId(String pblancId);

    // 그 기간에 접수기간이 걸치는 공고
    // superseded 는 정정공고에 대체된 것
    List<HousingNotice> findBySupersededFalseAndBeginDeLessThanEqualAndEndDeGreaterThanEqual(
            LocalDate monthEnd, LocalDate monthStart);

    // 지원금 상세에서 "관련 공고"를 찾을 때 쓴다. 마감 여부는 서비스에서 따로 거른다
    List<HousingNotice> findByTargetTypeAndSupersededFalse(TargetType targetType);
}
