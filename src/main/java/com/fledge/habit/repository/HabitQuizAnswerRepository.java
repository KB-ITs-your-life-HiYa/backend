package com.fledge.habit.repository;

import com.fledge.habit.domain.HabitQuizAnswer;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitQuizAnswerRepository extends JpaRepository<HabitQuizAnswer, Long> {

    Optional<HabitQuizAnswer> findByMemberIdAndAnsweredDate(Long memberId, LocalDate answeredDate);
}
