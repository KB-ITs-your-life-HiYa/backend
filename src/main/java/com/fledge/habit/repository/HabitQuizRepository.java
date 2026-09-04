package com.fledge.habit.repository;

import com.fledge.habit.domain.HabitQuiz;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitQuizRepository extends JpaRepository<HabitQuiz, Long> {

    List<HabitQuiz> findAllByOrderByIdAsc();
}
