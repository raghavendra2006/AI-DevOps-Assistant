from app.ml.classifier import InputClassifier
from app.ml.preprocessor import LogPreprocessor
from app.ml.command_risk import CommandRiskClassifier


def test_input_classifier_kubernetes():
    k8s_log = "Warning Back-off restarting failed container in pod checkout-api-9f8d exit code 137 OOMKilled"
    tech, conf = InputClassifier.classify(k8s_log)
    assert tech == "kubernetes"
    assert conf >= 0.7


def test_input_classifier_docker():
    docker_log = "Step 4/10 : RUN npm install\nfailed to solve with frontend dockerfile.v0"
    tech, conf = InputClassifier.classify(docker_log)
    assert tech == "docker"
    assert conf >= 0.7


def test_input_classifier_jenkins():
    jenkins_log = "[Pipeline] sh\n[Pipeline] maven\n[ERROR] Maven: BUILD FAILURE\nFinished: FAILURE"
    tech, conf = InputClassifier.classify(jenkins_log)
    assert tech == "jenkins"
    assert conf >= 0.7


def test_input_classifier_terraform():
    tf_log = "Error: Error acquiring the state lock: ConditionalCheckFailedException in terraform.tfstate"
    tech, conf = InputClassifier.classify(tf_log)
    assert tech == "terraform"
    assert conf >= 0.7


def test_input_classifier_generic():
    generic_log = "Uncaught SyntaxError: Unexpected token < in JSON at position 0"
    tech, conf = InputClassifier.classify(generic_log)
    assert tech == "generic"


def test_input_classifier_user_override():
    ambiguous_log = "Error in line 42"
    tech, conf = InputClassifier.classify(ambiguous_log, user_hint="terraform")
    assert tech == "terraform"
    assert conf == 1.0


def test_secret_redaction_comprehensive():
    raw_log = """
    Connecting to database with password=super_secret_pwd_123;
    Using AWS key AKIAIOSFODNN7EXAMPLE and secret aws_secret_access_key='wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY';
    Cloning repo with token ghp_111122223333444455556666777788889999;
    Incoming request from client IP 198.51.100.42 to authorization Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsK88;
    """
    sanitized, count = LogPreprocessor.redact_secrets(raw_log)
    assert count >= 5
    assert "super_secret_pwd_123" not in sanitized
    assert "AKIAIOSFODNN7EXAMPLE" not in sanitized
    assert "ghp_111122223333444455556666777788889999" not in sanitized
    assert "198.51.100.42" not in sanitized
    assert "[REDACTED_SECRET]" in sanitized
    assert "[REDACTED_AWS_KEY]" in sanitized
    assert "[REDACTED_GITHUB_TOKEN]" in sanitized
    assert "[REDACTED_IP]" in sanitized
    assert "[REDACTED_JWT]" in sanitized


def test_log_truncation():
    # Generate 300 lines of mock log
    lines = [f"2026-10-13 INFO Normal log line number {i}" for i in range(300)]
    lines[150] = "2026-10-13 FATAL: Critical exception occurred in transaction"
    huge_log = "\n".join(lines)

    truncated = LogPreprocessor.truncate_log(huge_log, max_lines=100)
    assert len(truncated.splitlines()) < 200
    assert "Critical exception occurred in transaction" in truncated
    assert "TRUNCATED" in truncated


def test_command_risk_classification():
    # Dangerous
    assert CommandRiskClassifier.classify_command("rm -rf /var/log/*") == "DANGEROUS"
    assert CommandRiskClassifier.classify_command("kubectl delete namespace prod --force") == "DANGEROUS"
    assert CommandRiskClassifier.classify_command("terraform destroy -auto-approve") == "DANGEROUS"
    assert CommandRiskClassifier.classify_command("docker system prune -a") == "DANGEROUS"

    # Caution
    assert CommandRiskClassifier.classify_command("kubectl rollout restart deployment auth-service") == "CAUTION"
    assert CommandRiskClassifier.classify_command("docker restart billing-api") == "CAUTION"
    assert CommandRiskClassifier.classify_command("terraform apply") == "CAUTION"

    # Safe
    assert CommandRiskClassifier.classify_command("kubectl describe pod payment-service-79") == "SAFE"
    assert CommandRiskClassifier.classify_command("docker logs -f my-container") == "SAFE"
    assert CommandRiskClassifier.classify_command("terraform plan") == "SAFE"
    assert CommandRiskClassifier.classify_command("cat /etc/hosts") == "SAFE"
