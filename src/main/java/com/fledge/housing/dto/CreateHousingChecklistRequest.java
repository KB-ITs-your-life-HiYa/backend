package com.fledge.housing.dto;

import com.fledge.housing.domain.ChecklistTemplateType;
import jakarta.validation.constraints.NotNull;

public record CreateHousingChecklistRequest(
        @NotNull ChecklistTemplateType templateType
) {}
