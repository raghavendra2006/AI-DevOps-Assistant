package com.aidevops.dto.response;

import com.aidevops.domain.RiskLevel;

public record RemediationCommandDto(
    Long id,
    String command,
    String description,
    RiskLevel riskLevel,
    Integer stepOrder
) {}
