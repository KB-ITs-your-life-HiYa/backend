package com.fledge.habit.dto;

public record HabitQuizResultResponse(Long selectedOptionId, boolean correct, String explanation) {
}
