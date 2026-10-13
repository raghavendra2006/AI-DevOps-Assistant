package com.aidevops.integration;

import com.aidevops.domain.InputType;
import com.aidevops.integration.ai.AiAnalyzeResponsePayload;
import com.aidevops.integration.ai.AiCircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiCircuitBreakerTest {

    private AiCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = new AiCircuitBreaker();
    }

    @Test
    void testInitialState_IsClosed() {
        assertEquals(AiCircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.canExecute());
    }

    @Test
    void testTripsToOpen_AfterConsecutiveFailures() {
        circuitBreaker.recordFailure(new RuntimeException("Connection timeout"));
        assertEquals(AiCircuitBreaker.State.CLOSED, circuitBreaker.getState());

        circuitBreaker.recordFailure(new RuntimeException("Connection timeout"));
        assertEquals(AiCircuitBreaker.State.CLOSED, circuitBreaker.getState());

        circuitBreaker.recordFailure(new RuntimeException("Connection timeout"));
        assertEquals(AiCircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertFalse(circuitBreaker.canExecute());
    }

    @Test
    void testRecordSuccess_ResetsFailures() {
        circuitBreaker.recordFailure(new RuntimeException("Network error"));
        circuitBreaker.recordSuccess();

        circuitBreaker.recordFailure(new RuntimeException("Network error"));
        assertEquals(AiCircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void testGenerateFallback_KubernetesOom() {
        String log = "Pod billing-api terminated with exit code 137 (OOMKilled)";
        AiAnalyzeResponsePayload fallback = circuitBreaker.generateFallback(log, InputType.KUBERNETES);

        assertNotNull(fallback);
        assertEquals("CRITICAL", fallback.severity());
        assertTrue(fallback.rootCause().contains("cgroup"));
        assertFalse(fallback.commands().isEmpty());
        assertEquals("offline-heuristic-fallback", fallback.providerUsed());
    }

    @Test
    void testGenerateFallback_TerraformLock() {
        String log = "Error: Error acquiring the state lock: ConditionalCheckFailedException";
        AiAnalyzeResponsePayload fallback = circuitBreaker.generateFallback(log, InputType.TERRAFORM);

        assertNotNull(fallback);
        assertEquals("HIGH", fallback.severity());
        assertTrue(fallback.summary().contains("State Lock"));
    }
}
