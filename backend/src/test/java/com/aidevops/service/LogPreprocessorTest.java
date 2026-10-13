package com.aidevops.service;

import com.aidevops.domain.InputType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogPreprocessorTest {

    private LogPreprocessorService service;

    @BeforeEach
    void setUp() {
        service = new LogPreprocessorService();
    }

    @Test
    void testDetectInputType_Kubernetes() {
        String log = "Back-off restarting failed container in pod auth-svc-789f exit code 137 OOMKilled";
        InputType type = service.detectInputType(log, null);
        assertEquals(InputType.KUBERNETES, type);
    }

    @Test
    void testDetectInputType_Docker() {
        String log = "Step 2/8 : RUN npm install\nfailed to solve with frontend dockerfile.v0";
        InputType type = service.detectInputType(log, null);
        assertEquals(InputType.DOCKER, type);
    }

    @Test
    void testDetectInputType_Jenkins() {
        String log = "[Pipeline] sh\n[Pipeline] maven: BUILD FAILURE\nFinished: FAILURE";
        InputType type = service.detectInputType(log, null);
        assertEquals(InputType.JENKINS, type);
    }

    @Test
    void testDetectInputType_Terraform() {
        String log = "Error: Error acquiring the state lock: ConditionalCheckFailedException in terraform.tfstate";
        InputType type = service.detectInputType(log, null);
        assertEquals(InputType.TERRAFORM, type);
    }

    @Test
    void testDetectInputType_Generic() {
        String log = "NullPointerException at com.example.Main.main(Main.java:14)";
        InputType type = service.detectInputType(log, null);
        assertEquals(InputType.GENERIC, type);
    }

    @Test
    void testDetectInputType_ExplicitHint() {
        String log = "random line";
        InputType type = service.detectInputType(log, "DOCKER");
        assertEquals(InputType.DOCKER, type);
    }

    @Test
    void testRedactSecrets_Comprehensive() {
        String raw = "password=SuperSecretPassword123\n" +
            "AWS Key: AKIAIOSFODNN7EXAMPLE\n" +
            "GitHub: ghp_111122223333444455556666777788889999\n" +
            "Client IP: 198.51.100.42\n" +
            "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsK88";

        LogPreprocessorService.PreprocessedLog result = service.preprocess(raw, null);

        assertTrue(result.redactedCount() >= 4);
        assertFalse(result.sanitizedLog().contains("SuperSecretPassword123"));
        assertFalse(result.sanitizedLog().contains("AKIAIOSFODNN7EXAMPLE"));
        assertFalse(result.sanitizedLog().contains("ghp_111122223333444455556666777788889999"));
        assertFalse(result.sanitizedLog().contains("198.51.100.42"));
        assertTrue(result.sanitizedLog().contains("[REDACTED_AWS_KEY]"));
        assertTrue(result.sanitizedLog().contains("[REDACTED_IP]"));
    }

    @Test
    void testTruncateLog_PreservesErrorContext() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            if (i == 100) {
                sb.append("FATAL EXCEPTION: OutOfMemoryError in thread pool\n");
            } else {
                sb.append("Normal application log line ").append(i).append("\n");
            }
        }

        String truncated = service.truncateLog(sb.toString());
        assertTrue(truncated.contains("FATAL EXCEPTION: OutOfMemoryError"));
        assertTrue(truncated.contains("TRUNCATED"));
    }
}
