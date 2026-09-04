package com.fledge.care.repository;

import com.fledge.care.domain.ReferralRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.Optional;

public interface ReferralRequestRepository extends JpaRepository<ReferralRequest, Long> {
    Optional<ReferralRequest> findFirstByCareSignalIdAndMemberIdAndStatusInOrderByIdDesc(
            Long signalId, Long memberId, Collection<String> statuses);
}
