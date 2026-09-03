package com.fledge.habit.repository;

import com.fledge.habit.domain.HabitPuzzleProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitPuzzleProgressRepository extends JpaRepository<HabitPuzzleProgress, Long> {

    List<HabitPuzzleProgress> findAllByMemberId(Long memberId);

    Optional<HabitPuzzleProgress> findByMemberIdAndPuzzleSetId(Long memberId, Long puzzleSetId);
}
