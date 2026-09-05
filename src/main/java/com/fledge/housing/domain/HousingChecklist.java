package com.fledge.housing.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "housing_checklist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousingChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 20)
    private ChecklistTemplateType templateType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<HousingChecklistItem> items = new ArrayList<>();

    public HousingChecklist(Long memberId, ChecklistTemplateType templateType) {
        this.memberId = memberId;
        this.templateType = templateType;
        this.createdAt = OffsetDateTime.now();
    }

    public void addItem(HousingChecklistItem item) {
        items.add(item);
        item.attach(this);
    }

    public void removeItem(HousingChecklistItem item) {
        items.remove(item);
        item.detach();
    }

    public int doneCount() {
        int n = 0;
        for (HousingChecklistItem item : items) {
            if (item.isDone()) n++;
        }
        return n;
    }
}
