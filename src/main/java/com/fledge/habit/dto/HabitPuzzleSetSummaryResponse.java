package com.fledge.habit.dto;

public record HabitPuzzleSetSummaryResponse(
        Long puzzleSetId,
        String title,
        String assetKey,
        int sortOrder,
        String status,
        int collectedPieces,
        int totalPieces
) {
}
