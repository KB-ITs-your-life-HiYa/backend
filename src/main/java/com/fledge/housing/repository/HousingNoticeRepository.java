package com.fledge.housing.repository;

import com.fledge.housing.domain.HousingNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HousingNoticeRepository extends JpaRepository<HousingNotice, Long> {

    Optional<HousingNotice> findByPblancId(String pblancId);
}
