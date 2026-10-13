package com.aidevops.service;

import com.aidevops.domain.InputType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogPreprocessorService {

    public record PreprocessedLog(
        String rawLog,
        String sanitizedLog,
        InputType inputType,
        int redactedCount,
        long sizeBytes
    ) {}

    private static final List<PatternReplacement> REDACTION_RULES = List.of(
        // Private Keys
        new PatternReplacement(
            Pattern.compile("-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----[\\s\\S]*?-----END (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"),
            "[REDACTED_PRIVATE_KEY]"
        ),
        // GitHub Tokens
        new PatternReplacement(
            Pattern.compile("\\bgh[pousr]_[A-Za-z0-9_]{36,}\\b"),
            "[REDACTED_GITHUB_TOKEN]"
        ),
        // AWS Access Key ID
        new PatternReplacement(
            Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
            "[REDACTED_AWS_KEY]"
        ),
        // AWS Secret Key
        new PatternReplacement(
            Pattern.compile("(?i)(?:aws_secret_access_key|aws_session_token)\\s*[=:]\\s*['\"]?([A-Za-z0-9/+=]{30,})['\"]?"),
            "aws_secret_access_key=[REDACTED_AWS_SECRET]"
        ),
        // JWT
        new PatternReplacement(
            Pattern.compile("\\bey[A-Za-z0-9_-]{15,}\\.ey[A-Za-z0-9_-]{15,}\\.[A-Za-z0-9_-]{15,}\\b"),
            "[REDACTED_JWT]"
        ),
        // Authorization Bearer
        new PatternReplacement(
            Pattern.compile("(?i)Authorization:\\s*Bearer\\s+([A-Za-z0-9_\\-\\.]{15,})"),
            "Authorization: Bearer [REDACTED_TOKEN]"
        ),
        // Key-value passwords / secrets
        new PatternReplacement(
            Pattern.compile("(?i)(password|passwd|pwd|api_key|apikey|client_secret)\\s*[:=]\\s*['\"]?([^\\s'\";,]{6,})['\"]?"),
            "$1=[REDACTED_SECRET]"
        ),
        // IPv4 (excluding localhost and zeros)
        new PatternReplacement(
            Pattern.compile("\\b(?!127\\.0\\.0\\.1\\b|0\\.0\\.0\\.0\\b)(?:\\d{1,3}\\.){3}\\d{1,3}\\b"),
            "[REDACTED_IP]"
        )
    );

    private record PatternReplacement(Pattern pattern, String replacement) {}

    public PreprocessedLog preprocess(String rawLog, String inputTypeHint) {
        if (rawLog == null) {
            rawLog = "";
        }

        InputType detectedType = detectInputType(rawLog, inputTypeHint);
        SanitizeResult sanitizeResult = redactSecrets(rawLog);
        String truncatedLog = truncateLog(sanitizeResult.text());

        return new PreprocessedLog(
            rawLog,
            truncatedLog,
            detectedType,
            sanitizeResult.redactedCount(),
            (long) rawLog.getBytes().length
        );
    }

    public InputType detectInputType(String text, String hint) {
        if (hint != null && !hint.isBlank() && !hint.equalsIgnoreCase("AUTO")) {
            try {
                return InputType.valueOf(hint.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to detection
            }
        }

        String lower = text.toLowerCase();
        int k8sScore = countOccurrences(lower, "crashloopbackoff", "oomkilled", "imagepullbackoff", "errimagepull", "exit code 137", "kubectl", "kubelet", "pod", "deployment");
        int dockerScore = countOccurrences(lower, "docker build", "failed to solve with frontend", "step ", "containerd", "dockerd", "docker-compose", "pull access denied");
        int jenkinsScore = countOccurrences(lower, "[pipeline]", "jenkinsfile", "started by user", "build failure", "finished: failure", "hudson.", "maven: build failure");
        int tfScore = countOccurrences(lower, "error acquiring the state lock", "terraform", "hashicorp/", "provider produced inconsistent result", "terraform.tfstate");

        int max = Math.max(Math.max(k8sScore, dockerScore), Math.max(jenkinsScore, tfScore));
        if (max == 0) {
            return InputType.GENERIC;
        }
        if (max == k8sScore) return InputType.KUBERNETES;
        if (max == dockerScore) return InputType.DOCKER;
        if (max == jenkinsScore) return InputType.JENKINS;
        return InputType.TERRAFORM;
    }

    private int countOccurrences(String text, String... keywords) {
        int count = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                count++;
            }
        }
        return count;
    }

    private record SanitizeResult(String text, int redactedCount) {}

    public SanitizeResult redactSecrets(String text) {
        if (text == null || text.isBlank()) {
            return new SanitizeResult("", 0);
        }

        String current = text;
        int totalRedactions = 0;

        for (PatternReplacement rule : REDACTION_RULES) {
            Matcher matcher = rule.pattern().matcher(current);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                totalRedactions += count;
                current = rule.pattern().matcher(current).replaceAll(rule.replacement());
            }
        }

        return new SanitizeResult(current, totalRedactions);
    }

    public String truncateLog(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = text.split("\\r?\\n");
        if (lines.length <= 150 && text.length() <= 15000) {
            return text;
        }

        int headCount = 20;
        int tailCount = 50;
        List<String> head = new ArrayList<>();
        List<String> tail = new ArrayList<>();
        List<String> errorLines = new ArrayList<>();

        for (int i = 0; i < Math.min(headCount, lines.length); i++) {
            head.add(lines[i]);
        }

        int tailStart = Math.max(headCount, lines.length - tailCount);
        for (int i = tailStart; i < lines.length; i++) {
            tail.add(lines[i]);
        }

        // Search for error context in middle
        for (int i = headCount; i < tailStart; i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("error") || line.contains("fatal") || line.contains("fail") || line.contains("exception") || line.contains("exit code")) {
                errorLines.add(lines[i]);
                if (errorLines.size() >= 30) break;
            }
        }

        int truncatedCount = lines.length - (head.size() + errorLines.size() + tail.size());
        StringBuilder sb = new StringBuilder();
        for (String h : head) sb.append(h).append("\n");
        sb.append("\n--- [TRUNCATED ").append(Math.max(0, truncatedCount)).append(" LINES | ERROR CONTEXT EXTRACTED] ---\n\n");
        for (String e : errorLines) sb.append(e).append("\n");
        if (!errorLines.isEmpty()) {
            sb.append("\n--- [RESUMING RECENT LOG TAIL] ---\n\n");
        }
        for (String t : tail) sb.append(t).append("\n");

        String result = sb.toString();
        if (result.length() > 15000) {
            result = result.substring(0, 15000) + "\n... [TRUNCATED BY CHARACTER LIMIT]";
        }
        return result;
    }
}
