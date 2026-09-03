package com.fledge.habit.repository;

import com.fledge.habit.domain.HabitPuzzleSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitPuzzleSetRepository extends JpaRepository<HabitPuzzleSet, Long> {
    List<HabitPuzzleSet> findAllByOrderBySortOrderAsc();
}
