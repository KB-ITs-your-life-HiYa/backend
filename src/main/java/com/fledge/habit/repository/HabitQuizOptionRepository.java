package com.fledge.habit.repository;

import com.fledge.habit.domain.HabitQuizOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitQuizOptionRepository extends JpaRepository<HabitQuizOption, Long> {

    List<HabitQuizOption> findByQuizIdOrderBySortOrderAsc(Long quizId);
}
