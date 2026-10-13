package com.aidevops.dto.response;

public record AnalysisReferenceDto(
    Long id,
    String title,
    String source,
    String docId,
    String url
) {}
