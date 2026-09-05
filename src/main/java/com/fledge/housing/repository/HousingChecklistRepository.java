package com.fledge.housing.repository;

import com.fledge.housing.domain.ChecklistTemplateType;
import com.fledge.housing.domain.HousingChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HousingChecklistRepository extends JpaRepository<HousingChecklist, Long> {

    List<HousingChecklist> findByMemberIdOrderByCreatedAtAsc(Long memberId);

    Optional<HousingChecklist> findByIdAndMemberId(Long id, Long memberId);

    boolean existsByMemberIdAndTemplateType(Long memberId, ChecklistTemplateType templateType);
}
