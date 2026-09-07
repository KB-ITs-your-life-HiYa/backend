package com.fledge.region.repository;

import com.fledge.region.domain.Sigungu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SigunguRepository extends JpaRepository<Sigungu, String> {
    // 이름만으로 조회하지 않는다 — '중구'처럼 여러 시/도에 중복되는 이름이 있다.
    Optional<Sigungu> findBySidoCodeAndName(String sidoCode, String name);
}
