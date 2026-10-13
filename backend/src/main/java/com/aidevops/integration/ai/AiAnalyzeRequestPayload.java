package com.aidevops.integration.ai;

import java.util.Map;

public record AiAnalyzeRequestPayload(
    String content,
    String input_type,
    Map<String, Object> context,
    int top_k
) {}
