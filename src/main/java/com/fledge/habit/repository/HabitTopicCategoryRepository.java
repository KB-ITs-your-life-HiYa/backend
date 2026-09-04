package com.fledge.habit.repository;

import com.fledge.habit.domain.HabitTopicCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitTopicCategoryRepository extends JpaRepository<HabitTopicCategory, Long> {

    List<HabitTopicCategory> findAllByOrderBySortOrderAsc();
}
