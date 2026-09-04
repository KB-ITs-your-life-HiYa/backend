package com.fledge.habit.dto;

public record HabitPuzzleProgressResponse(
        Long puzzleSetId,
        String title,
        String assetKey,
        int collectedPieces,
        int totalPieces,
        boolean completed,
        boolean allSetsCompleted
) {
}
