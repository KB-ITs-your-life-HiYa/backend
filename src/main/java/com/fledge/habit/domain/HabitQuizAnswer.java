package com.fledge.habit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "habit_quiz_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HabitQuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "selected_option_id")
    private Long selectedOptionId;

    @Column(name = "is_correct")
    private boolean correct;

    @Column(name = "answered_date")
    private LocalDate answeredDate;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public HabitQuizAnswer(Long memberId, Long quizId, Long selectedOptionId, boolean correct, LocalDate answeredDate) {
        this.memberId = memberId;
        this.quizId = quizId;
        this.selectedOptionId = selectedOptionId;
        this.correct = correct;
        this.answeredDate = answeredDate;
        this.createdAt = OffsetDateTime.now();
    }
}
