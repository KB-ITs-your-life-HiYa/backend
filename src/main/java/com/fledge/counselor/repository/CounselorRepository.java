package com.fledge.counselor.repository;

import com.fledge.counselor.domain.Counselor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CounselorRepository extends JpaRepository<Counselor, Long> {
    Optional<Counselor> findByMemberIdAndActiveTrue(Long memberId);
}
