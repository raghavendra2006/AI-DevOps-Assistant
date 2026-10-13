package com.aidevops.dto.response;

import com.aidevops.domain.IncidentStatus;
import com.aidevops.domain.InputType;
import com.aidevops.domain.Severity;
import java.time.Instant;
import java.util.List;

public record IncidentDetailResponse(
    Long id,
    String title,
    IncidentStatus status,
    Severity severity,
    InputType inputType,
    String summary,
    String rootCause,
    String impact,
    String explanation,
    Double confidence,
    String modelUsed,
    Boolean isFallback,
    String sanitizedLog,
    Integer redactedTokensCount,
    List<String> fixSteps,
    List<RemediationCommandDto> commands,
    List<AnalysisReferenceDto> references,
    Instant createdAt,
    Instant updatedAt
) {}
