package com.fledge.habit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "habit_quiz_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HabitQuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id")
    private Long quizId;

    private String label;

    @Column(name = "is_correct")
    private boolean correct;

    @Column(name = "sort_order")
    private int sortOrder;
}
