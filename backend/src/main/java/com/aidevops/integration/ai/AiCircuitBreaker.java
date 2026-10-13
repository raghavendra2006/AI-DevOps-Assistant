package com.aidevops.integration.ai;

import com.aidevops.domain.InputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AiCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(AiCircuitBreaker.class);

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold = 3;
    private final long resetTimeoutMillis = 30000; // 30s cooldown

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long lastStateChangedTimestamp = System.currentTimeMillis();

    public boolean canExecute() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }

        if (current == State.OPEN) {
            long elapsed = System.currentTimeMillis() - lastStateChangedTimestamp;
            if (elapsed > resetTimeoutMillis) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("Circuit breaker transitioned from OPEN to HALF_OPEN (probing AI service)");
                    return true;
                }
            }
            return false;
        }

        // HALF_OPEN allows single probe
        return true;
    }

    public void recordSuccess() {
        failureCount.set(0);
        if (state.get() != State.CLOSED) {
            log.info("AI service probe succeeded. Circuit breaker reset to CLOSED");
            state.set(State.CLOSED);
            lastStateChangedTimestamp = System.currentTimeMillis();
        }
    }

    public void recordFailure(Throwable cause) {
        int failures = failureCount.incrementAndGet();
        log.warn("AI service failure recorded (count={}/{}): {}", failures, failureThreshold, cause.getMessage());

        if (failures >= failureThreshold || state.get() == State.HALF_OPEN) {
            if (state.get() != State.OPEN) {
                state.set(State.OPEN);
                lastStateChangedTimestamp = System.currentTimeMillis();
                log.error("AI service failure threshold exceeded. Circuit breaker tripped to OPEN");
            }
        }
    }

    public State getState() {
        return state.get();
    }

    public void reset() {
        failureCount.set(0);
        state.set(State.CLOSED);
        lastStateChangedTimestamp = System.currentTimeMillis();
    }

    /**
     * Deterministic rule-based fallback analysis used when AI service is unavailable.
     */
    public AiAnalyzeResponsePayload generateFallback(String logContent, InputType inputType) {
        String lower = logContent != null ? logContent.toLowerCase() : "";

        if (lower.contains("137") || lower.contains("oomkilled")) {
            return new AiAnalyzeResponsePayload(
                "Container terminated due to Linux Out-Of-Memory (OOMKilled) condition (Offline Fallback Diagnosis).",
                "Process memory consumption exceeded the configured cgroup memory limit, triggering Linux kernel SIGKILL (exit code 137).",
                "CRITICAL",
                "Application pod repeatedly crash loops and fails readiness checks.",
                List.of(
                    "Inspect previous pod termination logs for high heap or memory leaks.",
                    "Increase container memory limit in deployment resource definitions.",
                    "Perform rolling deployment restart."
                ),
                List.of(
                    new AiAnalyzeResponsePayload.CommandItemPayload("kubectl describe pod <pod-name>", "Inspect container termination reason and exit code", "SAFE"),
                    new AiAnalyzeResponsePayload.CommandItemPayload("kubectl logs <pod-name> --previous", "Read last output lines prior to container SIGKILL", "SAFE"),
                    new AiAnalyzeResponsePayload.CommandItemPayload("kubectl rollout restart deployment <deployment-name>", "Restart pod replicas with clean memory", "CAUTION")
                ),
                "Heuristic analysis identified exit code 137 and OOMKilled markers. The AI microservice was unreachable, so this deterministic diagnosis was generated.",
                0.85,
                List.of(new AiAnalyzeResponsePayload.ReferenceItemPayload("Kubernetes Memory Troubleshooting", "knowledge_base", "kb-k8s-oom", null)),
                inputType.name().toLowerCase(),
                0,
                "offline-heuristic-fallback"
            );
        } else if (lower.contains("state lock") || lower.contains("conditionalcheckfailedexception")) {
            return new AiAnalyzeResponsePayload(
                "Terraform Remote State Lock Blocked (Offline Fallback Diagnosis).",
                "Another terraform process is executing or a prior process was abruptly terminated without unlocking DynamoDB/S3 state.",
                "HIGH",
                "CI/CD infrastructure provisioning pipeline blocked from applying changes.",
                List.of(
                    "Verify no team members or CI jobs are actively running terraform apply.",
                    "If orphaned, obtain the Lock ID from error and run force-unlock."
                ),
                List.of(
                    new AiAnalyzeResponsePayload.CommandItemPayload("terraform plan", "Verify state lock status", "SAFE"),
                    new AiAnalyzeResponsePayload.CommandItemPayload("terraform force-unlock <LOCK_ID>", "Release orphaned state lock", "CAUTION")
                ),
                "Heuristic analysis identified state lock contention. Generated via offline rule engine.",
                0.85,
                List.of(new AiAnalyzeResponsePayload.ReferenceItemPayload("Terraform State Recovery", "knowledge_base", "kb-tf-lock", null)),
                inputType.name().toLowerCase(),
                0,
                "offline-heuristic-fallback"
            );
        } else {
            return new AiAnalyzeResponsePayload(
                "DevOps Incident Triage (Offline Fallback Diagnosis).",
                "Unclassified failure detected in logs. The AI service is currently unavailable.",
                "MEDIUM",
                "System experienced an unhandled error or pipeline failure.",
                List.of(
                    "Examine recent system changes, commits, or container build layers.",
                    "Verify network connectivity and service dependencies."
                ),
                List.of(
                    new AiAnalyzeResponsePayload.CommandItemPayload("tail -n 100 /var/log/syslog", "Inspect system error logs", "SAFE")
                ),
                "Automatic fallback diagnosis generated because the AI microservice is currently unreachable.",
                0.60,
                List.of(),
                inputType.name().toLowerCase(),
                0,
                "offline-heuristic-fallback"
            );
        }
    }
}
