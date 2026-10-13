package com.aidevops.dto.response;

import com.aidevops.domain.IncidentStatus;
import com.aidevops.domain.InputType;
import com.aidevops.domain.Severity;
import java.time.Instant;

public record IncidentSummaryResponse(
    Long id,
    String title,
    IncidentStatus status,
    Severity severity,
    InputType inputType,
    String summary,
    Double confidence,
    Boolean isFallback,
    Instant createdAt
) {}
