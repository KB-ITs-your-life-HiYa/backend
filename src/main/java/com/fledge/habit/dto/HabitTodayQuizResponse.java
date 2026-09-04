package com.fledge.habit.dto;

import java.util.List;

/**
 * GET /habit/quiz/today 응답.
 */
public record HabitTodayQuizResponse(
        Long quizId,
        String question,
        List<HabitQuizOptionResponse> options,
        boolean answered,
        HabitQuizResultResponse result
) {
}
