package com.fledge.habit.dto;

import jakarta.validation.constraints.NotNull;

public record HabitQuizAnswerRequest(@NotNull Long optionId) {
}
