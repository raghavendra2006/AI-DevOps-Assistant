import re
from typing import Dict, List, Tuple


class InputClassifier:
    """
    Rapid deterministic and heuristic signature classifier for DevOps log and manifest types:
    Kubernetes, Docker, Jenkins, Terraform, or Generic.
    """

    PATTERNS: Dict[str, List[re.Pattern]] = {
        "kubernetes": [
            re.compile(r"CrashLoopBackOff", re.IGNORECASE),
            re.compile(r"OOMKilled", re.IGNORECASE),
            re.compile(r"ImagePullBackOff", re.IGNORECASE),
            re.compile(r"ErrImagePull", re.IGNORECASE),
            re.compile(r"exit code 137", re.IGNORECASE),
            re.compile(r"Back-off restarting failed container", re.IGNORECASE),
            re.compile(r"\bkubectl\b", re.IGNORECASE),
            re.compile(r"\bkubelet\b", re.IGNORECASE),
            re.compile(r"\bpod\b.*(?:failed|evicted|terminating)", re.IGNORECASE),
            re.compile(r"apiVersion:\s*apps/v1", re.IGNORECASE),
            re.compile(r"kind:\s*(?:Deployment|StatefulSet|DaemonSet|Pod|Service)", re.IGNORECASE),
        ],
        "docker": [
            re.compile(r"docker build", re.IGNORECASE),
            re.compile(r"failed to solve with frontend dockerfile", re.IGNORECASE),
            re.compile(r"Step \d+/\d+\s*:", re.IGNORECASE),
            re.compile(r"OCI runtime exec failed", re.IGNORECASE),
            re.compile(r"containerd", re.IGNORECASE),
            re.compile(r"dockerd", re.IGNORECASE),
            re.compile(r"docker-compose", re.IGNORECASE),
            re.compile(r"pull access denied for", re.IGNORECASE),
            re.compile(r"non-zero code:\s*\d+", re.IGNORECASE),
        ],
        "jenkins": [
            re.compile(r"\[Pipeline\]", re.IGNORECASE),
            re.compile(r"Jenkinsfile", re.IGNORECASE),
            re.compile(r"Started by user", re.IGNORECASE),
            re.compile(r"BUILD FAILURE", re.IGNORECASE),
            re.compile(r"Finished: FAILURE", re.IGNORECASE),
            re.compile(r"hudson\.", re.IGNORECASE),
            re.compile(r"FATAL: Unable to mark build", re.IGNORECASE),
            re.compile(r"Maven: BUILD FAILURE", re.IGNORECASE),
            re.compile(r"gradlew.*failed", re.IGNORECASE),
        ],
        "terraform": [
            re.compile(r"Error acquiring the state lock", re.IGNORECASE),
            re.compile(r"Error releasing the state lock", re.IGNORECASE),
            re.compile(r"terraform (?:init|plan|apply|destroy)", re.IGNORECASE),
            re.compile(r"hashicorp/", re.IGNORECASE),
            re.compile(r"Provider produced inconsistent result", re.IGNORECASE),
            re.compile(r"resource \"[a-zA-Z0-9_-]+\"", re.IGNORECASE),
            re.compile(r"Backend configuration changed", re.IGNORECASE),
            re.compile(r"Cycle:\s*module\.", re.IGNORECASE),
            re.compile(r"terraform\.tfstate", re.IGNORECASE),
        ],
    }

    @classmethod
    def classify(cls, text: str, user_hint: str = None) -> Tuple[str, float]:
        """
        Classifies input text into one of: kubernetes, docker, jenkins, terraform, or generic.
        Returns a tuple of (detected_type, confidence_score).
        """
        if user_hint and user_hint.lower() in ("kubernetes", "docker", "jenkins", "terraform"):
            return user_hint.lower(), 1.0

        if not text or not text.strip():
            return "generic", 0.0

        scores: Dict[str, int] = {k: 0 for k in cls.PATTERNS.keys()}

        for tech, patterns in cls.PATTERNS.items():
            for pat in patterns:
                matches = pat.findall(text)
                if matches:
                    scores[tech] += len(matches)

        best_tech = "generic"
        best_score = 0

        for tech, score in scores.items():
            if score > best_score:
                best_score = score
                best_tech = tech

        if best_score == 0:
            return "generic", 0.5

        # Normalize score
        confidence = min(0.95, 0.6 + (best_score * 0.1))
        return best_tech, round(confidence, 2)
