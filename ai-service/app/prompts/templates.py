"""Few-shot in-context learning demonstrations for DevOps tasks."""

FEW_SHOT_LOG_ANALYSIS = """
### EXAMPLE 1:
Input Log:
"FATAL: [137] OOMKilled - container failed due to memory limit 512Mi in pod billing-api-79dfb"
Output:
{
  "task": "log_analysis",
  "root_cause": "Container terminated with Linux SIGKILL (exit code 137) triggered by kernel Out-Of-Memory (OOM) killer exceeding 512Mi limit.",
  "severity": "CRITICAL",
  "affected_component": "billing-api",
  "recommended_actions": [
    "Increase pod resources.limits.memory to 1Gi or 1.5Gi in deployment manifest",
    "Inspect JVM heap sizing (-Xmx) to ensure it is configured within container cgroup limits",
    "Review memory profile for potential memory leaks under load"
  ],
  "confidence_score": 0.98
}

### EXAMPLE 2:
Input Log:
"Error: Error acquiring the state lock: 2 errors occurred: ConditionalCheckFailedException: The conditional request failed"
Output:
{
  "task": "log_analysis",
  "root_cause": "Terraform DynamoDB state lock contention. Another terraform apply process is currently running or a prior run crashed without releasing the lock.",
  "severity": "HIGH",
  "affected_component": "terraform_state",
  "recommended_actions": [
    "Verify with team if an active terraform apply is executing",
    "If lock was orphaned by an interrupted process, run 'terraform force-unlock <LOCK_ID>' after confirming no concurrency"
  ],
  "confidence_score": 0.95
}
"""
