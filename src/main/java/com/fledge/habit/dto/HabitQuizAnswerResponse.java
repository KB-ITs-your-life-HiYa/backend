package com.fledge.habit.dto;

/**
 * POST /habit/quiz/today/answer 응답.
 */
public record HabitQuizAnswerResponse(
        boolean correct,
        String explanation,
        HabitPuzzleProgressResponse progress,
        boolean justCompleted
) {
}
