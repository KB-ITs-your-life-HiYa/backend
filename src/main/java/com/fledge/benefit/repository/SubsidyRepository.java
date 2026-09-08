package com.fledge.benefit.repository;

import com.fledge.benefit.domain.Subsidy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubsidyRepository extends JpaRepository<Subsidy, Long> {
    boolean existsByRawId(Long rawId);

    // "받고 있는 지원금 추가" 화면의 검색용. 이름에 검색어가 포함된 것만, 최대 개수는 Pageable 로 제한
    List<Subsidy> findByNameContainingIgnoreCase(String name, Pageable pageable);
}