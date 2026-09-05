package com.fledge.housing.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "housing_checklist_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousingChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private HousingChecklist checklist;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public HousingChecklistItem(String content, String memo, int sortOrder) {
        this.content = content;
        this.memo = memo;
        this.done = false;
        this.sortOrder = sortOrder;
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void attach(HousingChecklist checklist) {
        this.checklist = checklist;
    }

    void detach() {
        this.checklist = null;
    }

    /**
     * PATCH 시 전달된 필드만 반영한다.
     * dueDate/memo 는 present=true 이면 null 로도 지울 수 있다.
     */
    public void applyPatch(String content, boolean dueDatePresent, LocalDate dueDate,
                           boolean memoPresent, String memo,
                           Boolean done, Integer sortOrder) {
        if (content != null) this.content = content;
        if (dueDatePresent) this.dueDate = dueDate;
        if (memoPresent) this.memo = memo;
        if (done != null) this.done = done;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = OffsetDateTime.now();
    }
}
