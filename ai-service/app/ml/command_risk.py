import re
from typing import List, Tuple


class CommandRiskClassifier:
    """
    Deterministic security guardrail that classifies shell / CLI commands into:
    - DANGEROUS: High risk of data loss, irreversible infrastructure destruction, or forced termination.
    - CAUTION: Modifies live state, restarts services, scales workloads, or applies changes.
    - SAFE: Read-only inspection, log fetching, cluster status queries, or verification.
    """

    DANGEROUS_PATTERNS: List[re.Pattern] = [
        re.compile(r"\brm\s+-(?:[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*f[a-zA-Z]*r)\b", re.IGNORECASE),
        re.compile(r"\brm\s+--recursive\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+delete\s+.*(?:--force|--grace-period=0)\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+delete\s+(?:--all|namespace\b|ns\b|\ball\b)", re.IGNORECASE),
        re.compile(r"\bdocker\s+system\s+prune\b", re.IGNORECASE),
        re.compile(r"\bdocker\s+rm\s+-[a-zA-Z]*f\b", re.IGNORECASE),
        re.compile(r"\bdocker\s+rmi\s+-[a-zA-Z]*f\b", re.IGNORECASE),
        re.compile(r"\bterraform\s+destroy\b", re.IGNORECASE),
        re.compile(r"\bterraform\s+state\s+rm\b", re.IGNORECASE),
        re.compile(r"\bdrop\s+(?:database|table)\b", re.IGNORECASE),
        re.compile(r"\btruncate\s+(?:table)?\b", re.IGNORECASE),
        re.compile(r"\bkill\s+-9\s+1\b", re.IGNORECASE),
        re.compile(r"\bmkfs\b", re.IGNORECASE),
        re.compile(r"\bdd\s+if=", re.IGNORECASE),
        re.compile(r"--no-preserve-root", re.IGNORECASE),
    ]

    CAUTION_PATTERNS: List[re.Pattern] = [
        re.compile(r"\bkubectl\s+rollout\s+restart\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+scale\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+apply\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+patch\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+edit\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+set\s+(?:resources|image)\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+(?:drain|cordon)\b", re.IGNORECASE),
        re.compile(r"\bkubectl\s+delete\s+pod\b", re.IGNORECASE),
        re.compile(r"\bdocker\s+(?:restart|stop|kill)\b", re.IGNORECASE),
        re.compile(r"\bterraform\s+apply\b", re.IGNORECASE),
        re.compile(r"\bterraform\s+import\b", re.IGNORECASE),
        re.compile(r"\bsystemctl\s+(?:restart|stop|reload)\b", re.IGNORECASE),
        re.compile(r"\biptables\b", re.IGNORECASE),
    ]

    @classmethod
    def classify_command(cls, command: str, llm_suggestion: str = None) -> str:
        """
        Classifies risk level for a command. Enforces deterministic safety overrides:
        Dangerous rules strictly override any weaker assessment.
        """
        if not command or not command.strip():
            return "SAFE"

        cmd_clean = command.strip()

        # Check dangerous first
        for pat in cls.DANGEROUS_PATTERNS:
            if pat.search(cmd_clean):
                return "DANGEROUS"

        # Check caution
        for pat in cls.CAUTION_PATTERNS:
            if pat.search(cmd_clean):
                return "CAUTION"

        # If LLM tagged CAUTION or DANGEROUS and we didn't rule it dangerous, respect higher caution
        if llm_suggestion and llm_suggestion.upper() in ("DANGEROUS", "CAUTION"):
            return llm_suggestion.upper()

        return "SAFE"
