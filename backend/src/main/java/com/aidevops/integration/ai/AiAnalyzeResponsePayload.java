package com.aidevops.integration.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiAnalyzeResponsePayload(
    String summary,
    @JsonProperty("root_cause") String rootCause,
    String severity,
    String impact,
    @JsonProperty("fix_steps") List<String> fixSteps,
    List<CommandItemPayload> commands,
    String explanation,
    Double confidence,
    List<ReferenceItemPayload> references,
    @JsonProperty("detected_input_type") String detectedInputType,
    @JsonProperty("sanitized_tokens_count") Integer sanitizedTokensCount,
    @JsonProperty("provider_used") String providerUsed
) {
    public record CommandItemPayload(
        String command,
        String description,
        @JsonProperty("risk_level") String riskLevel
    ) {}

    public record ReferenceItemPayload(
        String title,
        String source,
        @JsonProperty("doc_id") String docId,
        String url
    ) {}
}
