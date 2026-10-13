package com.aidevops.integration.ai;

import com.aidevops.domain.InputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient restClient;
    private final AiCircuitBreaker circuitBreaker;
    private final String apiKey;

    public AiServiceClient(
        @Value("${app.ai-service.url:http://localhost:8000}") String aiServiceUrl,
        @Value("${app.ai-service.api-key:devops-ai-secret-key-2026}") String apiKey,
        @Value("${app.ai-service.connect-timeout-ms:5000}") int connectTimeout,
        @Value("${app.ai-service.read-timeout-ms:45000}") int readTimeout,
        AiCircuitBreaker circuitBreaker
    ) {
        this.apiKey = apiKey;
        this.circuitBreaker = circuitBreaker;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = RestClient.builder()
            .baseUrl(aiServiceUrl)
            .requestFactory(requestFactory)
            .build();
    }

    public AnalysisResultWrapper analyze(String content, InputType inputType, java.util.Map<String, Object> context) {
        String correlationId = UUID.randomUUID().toString();

        if (!circuitBreaker.canExecute()) {
            log.warn("AI service circuit breaker is OPEN. Invoking offline heuristic fallback for correlationId={}", correlationId);
            AiAnalyzeResponsePayload fallback = circuitBreaker.generateFallback(content, inputType);
            return new AnalysisResultWrapper(fallback, true);
        }

        try {
            log.info("Dispatching incident analysis to AI service (correlationId={}, inputType={})", correlationId, inputType);

            AiAnalyzeRequestPayload requestPayload = new AiAnalyzeRequestPayload(
                content,
                inputType.name().toLowerCase(),
                context,
                4
            );

            AiAnalyzeResponsePayload response = restClient.post()
                .uri("/api/v1/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", apiKey)
                .header("X-Correlation-ID", correlationId)
                .body(requestPayload)
                .retrieve()
                .body(AiAnalyzeResponsePayload.class);

            if (response != null) {
                circuitBreaker.recordSuccess();
                return new AnalysisResultWrapper(response, false);
            } else {
                throw new IllegalStateException("Empty response received from AI service");
            }
        } catch (Exception ex) {
            log.error("AI service execution failed for correlationId={}: {}", correlationId, ex.getMessage());
            circuitBreaker.recordFailure(ex);
            AiAnalyzeResponsePayload fallback = circuitBreaker.generateFallback(content, inputType);
            return new AnalysisResultWrapper(fallback, true);
        }
    }

    public record AnalysisResultWrapper(
        AiAnalyzeResponsePayload payload,
        boolean isFallback
    ) {}
}
