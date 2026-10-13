import re
from typing import List, Tuple


class LogPreprocessor:
    """
    Sanitizes raw DevOps logs:
    1. Redacts secrets, tokens, private keys, passwords, and IP addresses.
    2. Smartly truncates massive logs while prioritizing error windows and stack traces.
    """

    REDACTION_RULES: List[Tuple[re.Pattern, str]] = [
        # Private Keys
        (
            re.compile(
                r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----[\s\S]*?-----END (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"
            ),
            "[REDACTED_PRIVATE_KEY]",
        ),
        # GitHub Personal Access Tokens
        (re.compile(r"\bgh[pousr]_[A-Za-z0-9_]{36,}\b"), "[REDACTED_GITHUB_TOKEN]"),
        # AWS Access Key ID
        (re.compile(r"\bAKIA[0-9A-Z]{16}\b"), "[REDACTED_AWS_KEY]"),
        # AWS Secret Access Key
        (
            re.compile(
                r"(?i)(?:aws_secret_access_key|aws_session_token)\s*[=:]\s*['\"]?([A-Za-z0-9/+=]{30,})['\"]?"
            ),
            "[REDACTED_AWS_SECRET]",
        ),
        # JSON Web Tokens (JWT)
        (
            re.compile(r"\bey[A-Za-z0-9_-]{15,}\.ey[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\b"),
            "[REDACTED_JWT]",
        ),
        # Authorization Bearer tokens
        (
            re.compile(r"(?i)Authorization:\s*Bearer\s+([A-Za-z0-9_\-\.]{15,})"),
            "Authorization: Bearer [REDACTED_TOKEN]",
        ),
        # Passwords and general secrets in key-value formats
        (
            re.compile(
                r"(?i)(password|passwd|pwd|api_key|apikey|client_secret)\s*[:=]\s*['\"]?([^\s'\";,]{6,})['\"]?"
            ),
            r"\1=[REDACTED_SECRET]",
        ),
        # IPv4 Addresses (exclude localhost and zero)
        (
            re.compile(r"\b(?!127\.0\.0\.1\b|0\.0\.0\.0\b)(?:\d{1,3}\.){3}\d{1,3}\b"),
            "[REDACTED_IP]",
        ),
    ]

    ERROR_LINE_INDICATORS = [
        "error",
        "fatal",
        "fail",
        "exception",
        "exit code",
        "traceback",
        "caused by:",
        "panic:",
        "crashloopbackoff",
        "oomkilled",
        "denied",
    ]

    @classmethod
    def redact_secrets(cls, text: str) -> Tuple[str, int]:
        """
        Redacts sensitive tokens, credentials, and IP addresses.
        Returns the sanitized text and total count of redacted items.
        """
        if not text:
            return "", 0

        sanitized = text
        total_redactions = 0

        for pattern, replacement in cls.REDACTION_RULES:
            matches = pattern.findall(sanitized)
            if matches:
                total_redactions += len(matches)
                sanitized = pattern.sub(replacement, sanitized)

        return sanitized, total_redactions

    @classmethod
    def extract_error_lines(cls, text: str) -> List[str]:
        """
        Extracts lines containing error keywords or stack trace indications.
        """
        lines = text.splitlines()
        error_lines = []
        for line in lines:
            line_lower = line.lower()
            if any(indicator in line_lower for indicator in cls.ERROR_LINE_INDICATORS):
                error_lines.append(line.strip())
        return error_lines

    @classmethod
    def truncate_log(cls, text: str, max_lines: int = 120, max_chars: int = 15000) -> str:
        """
        Smartly truncates large logs while preserving head lines, tail lines, and error windows.
        """
        if not text:
            return ""

        lines = text.splitlines()
        total_lines = len(lines)

        if total_lines <= max_lines and len(text) <= max_chars:
            return text

        # Select head
        head_count = 20
        tail_count = 50
        head_lines = lines[:head_count]
        tail_lines = lines[-tail_count:] if total_lines > tail_count else []

        # Find error indices in middle lines
        middle_lines = lines[head_count:-tail_count] if total_lines > (head_count + tail_count) else []
        highlighted_middle = []
        for i, line in enumerate(middle_lines):
            line_lower = line.lower()
            if any(indicator in line_lower for indicator in cls.ERROR_LINE_INDICATORS):
                # Grab ±1 line context
                start = max(0, i - 1)
                end = min(len(middle_lines), i + 2)
                for ctx_idx in range(start, end):
                    if middle_lines[ctx_idx] not in highlighted_middle:
                        highlighted_middle.append(middle_lines[ctx_idx])
            if len(highlighted_middle) >= 40:
                break

        truncated_count = total_lines - (len(head_lines) + len(highlighted_middle) + len(tail_lines))
        marker = f"\n--- [TRUNCATED {max(0, truncated_count)} INTERMEDIATE LINES | PRESERVED ERROR CONTEXT] ---\n"

        result = (
            "\n".join(head_lines)
            + marker
            + "\n".join(highlighted_middle)
            + ("\n" + marker if highlighted_middle else "")
            + "\n".join(tail_lines)
        )

        if len(result) > max_chars:
            result = result[:max_chars] + "\n... [TRUNCATED BY CHARACTER LIMIT]"

        return result
