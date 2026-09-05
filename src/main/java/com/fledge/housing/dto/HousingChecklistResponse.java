package com.fledge.housing.dto;

import com.fledge.housing.domain.ChecklistTemplateType;
import com.fledge.housing.domain.HousingChecklist;
import com.fledge.housing.domain.HousingChecklistItem;

import java.time.LocalDate;
import java.util.List;

public record HousingChecklistResponse(
        Long id,
        ChecklistTemplateType templateType,
        String title,
        int doneCount,
        int totalCount,
        double progress,
        List<HousingChecklistItemResponse> items
) {
    public static HousingChecklistResponse from(HousingChecklist checklist) {
        List<HousingChecklistItemResponse> items = checklist.getItems().stream()
                .map(HousingChecklistItemResponse::from)
                .toList();
        int total = items.size();
        int done = checklist.doneCount();
        double progress = total == 0 ? 0.0 : (double) done / total;
        return new HousingChecklistResponse(
                checklist.getId(),
                checklist.getTemplateType(),
                checklist.getTemplateType().title(),
                done,
                total,
                progress,
                items
        );
    }

    public record HousingChecklistItemResponse(
            Long id,
            String content,
            LocalDate dueDate,
            String memo,
            boolean done,
            int sortOrder
    ) {
        public static HousingChecklistItemResponse from(HousingChecklistItem item) {
            return new HousingChecklistItemResponse(
                    item.getId(),
                    item.getContent(),
                    item.getDueDate(),
                    item.getMemo(),
                    item.isDone(),
                    item.getSortOrder()
            );
        }
    }
}
