package com.fledge.housing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateHousingChecklistItemRequest(
        @NotBlank @Size(max = 500) String content,
        LocalDate dueDate,
        @Size(max = 2000) String memo
) {}
