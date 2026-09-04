package com.fledge.habit.repository;

import com.fledge.habit.domain.HabitTopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitTopicRepository extends JpaRepository<HabitTopic, Long> {

    List<HabitTopic> findAllByCategoryIdOrderBySortOrderAsc(Long categoryId);
}
