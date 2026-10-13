package com.aidevops.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record IncidentAnalyzeRequest(
    @NotBlank(message = "Log content cannot be blank")
    String content,

    String title,

    String inputType, // KUBERNETES, DOCKER, JENKINS, TERRAFORM, GENERIC, or AUTO

    Map<String, Object> context // optional environment, cluster, service
) {}
