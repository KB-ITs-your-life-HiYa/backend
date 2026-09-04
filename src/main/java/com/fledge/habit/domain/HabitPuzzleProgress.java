package com.fledge.habit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "habit_puzzle_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HabitPuzzleProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "puzzle_set_id")
    private Long puzzleSetId;

    @Column(name = "collected_pieces")
    private int collectedPieces;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public HabitPuzzleProgress(Long memberId, Long puzzleSetId) {
        this.memberId = memberId;
        this.puzzleSetId = puzzleSetId;
        this.collectedPieces = 0;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public void addPiece(int totalPieces) {
        if (collectedPieces >= totalPieces) {
            return;
        }
        collectedPieces += 1;
        updatedAt = OffsetDateTime.now();
        if (collectedPieces >= totalPieces) {
            completedAt = OffsetDateTime.now();
        }
    }
}
