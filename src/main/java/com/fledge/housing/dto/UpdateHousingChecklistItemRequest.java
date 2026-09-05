package com.fledge.housing.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 항목 부분 수정. dueDate / memo 는 JSON 에 키가 있으면 null 로도 지울 수 있다.
 */
public record UpdateHousingChecklistItemRequest(
        @Size(max = 500) String content,
        JsonNode dueDate,
        JsonNode memo,
        Boolean done,
        Integer sortOrder
) {
    @JsonIgnore
    public boolean dueDatePresent() {
        return dueDate != null;
    }

    @JsonIgnore
    public LocalDate dueDateValue() {
        if (dueDate == null || dueDate.isNull()) return null;
        return LocalDate.parse(dueDate.asText());
    }

    @JsonIgnore
    public boolean memoPresent() {
        return memo != null;
    }

    @JsonIgnore
    public String memoValue() {
        if (memo == null || memo.isNull()) return null;
        return memo.asText();
    }
}
