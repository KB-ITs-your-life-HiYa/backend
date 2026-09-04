package com.fledge.care.repository;

import com.fledge.care.domain.CareResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareResponseRepository extends JpaRepository<CareResponse, Long> {
    java.util.List<CareResponse> findByCareSignalIdOrderByCreatedAtAscIdAsc(Long signalId);
    java.util.Optional<CareResponse> findByCareSignalIdAndRequestId(Long signalId, String requestId);
    java.util.Optional<CareResponse> findByIdAndCareSignalId(Long id, Long signalId);
}
