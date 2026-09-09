package com.fledge.counselor.repository;

import com.fledge.counselor.domain.CounselorYouthAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CounselorYouthAssignmentRepository extends JpaRepository<CounselorYouthAssignment, Long> {
    @Query(value = """
            SELECT assignment.*
            FROM counselor_youth_assignment assignment
            JOIN counselor counselor ON counselor.id = assignment.counselor_id
            WHERE assignment.youth_member_id = :youthMemberId
              AND assignment.unassigned_at IS NULL
              AND counselor.is_active = true
            """, nativeQuery = true)
    Optional<CounselorYouthAssignment> findActiveByYouthMemberId(
            @Param("youthMemberId") Long youthMemberId);
}
