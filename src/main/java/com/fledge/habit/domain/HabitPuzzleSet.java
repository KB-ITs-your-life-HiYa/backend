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
@Table(name = "habit_puzzle_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HabitPuzzleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "asset_key")
    private String assetKey;

    @Column(name = "total_pieces")
    private int totalPieces;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
