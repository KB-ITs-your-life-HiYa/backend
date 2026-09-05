package com.fledge.housing.repository;

import com.fledge.housing.domain.HousingChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HousingChecklistItemRepository extends JpaRepository<HousingChecklistItem, Long> {

    @Query("select coalesce(max(i.sortOrder), -1) from HousingChecklistItem i where i.checklist.id = :checklistId")
    int findMaxSortOrder(@Param("checklistId") Long checklistId);
}
