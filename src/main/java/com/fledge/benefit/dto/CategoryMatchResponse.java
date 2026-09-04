package com.fledge.benefit.dto;

import java.util.List;

public record CategoryMatchResponse(
        String category,
        List<SubsidyMatchResponse> items
) {
}