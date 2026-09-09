package com.fledge.counselor.repository;

import com.fledge.counselor.domain.CounselorYouthAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query(value = """
            SELECT assignment.*
            FROM counselor_youth_assignment assignment
            JOIN member youth ON youth.id = assignment.youth_member_id
            WHERE assignment.counselor_id = :counselorId
              AND assignment.unassigned_at IS NULL
              AND youth.role = 'YOUTH'
            ORDER BY youth.id ASC
            """, nativeQuery = true)
    List<CounselorYouthAssignment> findActiveByCounselorId(
            @Param("counselorId") Long counselorId);
}
