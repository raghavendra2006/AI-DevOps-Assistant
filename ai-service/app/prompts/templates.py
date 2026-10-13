"""Few-shot in-context learning demonstrations for DevOps Incident Analysis."""

FEW_SHOT_LOG_ANALYSIS = """
### EXAMPLE 1:
Input Log:
"FATAL: [137] OOMKilled - container failed due to memory limit 512Mi in pod billing-api-79dfb"
Output:
{
  "summary": "Pod billing-api-79dfb was abruptly killed by the Linux OOM killer after exceeding container memory limit.",
  "root_cause": "Process allocated memory beyond cgroup limit (512Mi), triggering OS kernel SIGKILL (exit code 137).",
  "severity": "CRITICAL",
  "impact": "Pod is in CrashLoopBackOff, dropping billing requests and failing liveness checks.",
  "fix_steps": [
    "Inspect previous container logs to observe heap or memory consumption prior to death.",
    "Increase pod memory limit in deployment manifest from 512Mi to 1.5Gi.",
    "Perform rolling rollout restart of the deployment."
  ],
  "commands": [
    {
      "command": "kubectl describe pod billing-api-79dfb -n default",
      "description": "Inspect termination reason, exit code, and previous state",
      "risk_level": "SAFE"
    },
    {
      "command": "kubectl logs billing-api-79dfb --previous --tail=100",
      "description": "Fetch final application log lines before SIGKILL",
      "risk_level": "SAFE"
    },
    {
      "command": "kubectl set resources deployment billing-api --limits=memory=1.5Gi --requests=memory=768Mi",
      "description": "Update memory limits to allow sufficient headroom",
      "risk_level": "CAUTION"
    }
  ],
  "explanation": "Exit code 137 represents 128 + 9 (SIGKILL). In Kubernetes environments, cgroups enforce limits; exceeding them forces an uncatchable SIGKILL.",
  "confidence": 0.98,
  "references": []
}

### EXAMPLE 2:
Input Log:
"Error: Error acquiring the state lock: 2 errors occurred: ConditionalCheckFailedException: Lock Info: ID: 3e79-bc91-22"
Output:
{
  "summary": "Terraform state operation blocked by active lock in remote backend.",
  "root_cause": "Terraform remote state backend (DynamoDB/S3) is locked by another running or crashed process (Lock ID: 3e79-bc91-22).",
  "severity": "HIGH",
  "impact": "CI/CD infrastructure provisioning pipeline blocked from applying changes.",
  "fix_steps": [
    "Verify no other team member or CI/CD job is actively running terraform apply.",
    "If lock was orphaned by an aborted execution, force-unlock using the lock ID.",
    "Re-run terraform plan to verify clean state."
  ],
  "commands": [
    {
      "command": "terraform plan",
      "description": "Verify whether state is still locked or accessible",
      "risk_level": "SAFE"
    },
    {
      "command": "terraform force-unlock 3e79-bc91-22",
      "description": "Release the orphaned DynamoDB state lock",
      "risk_level": "CAUTION"
    }
  ],
  "explanation": "Terraform employs state locking to prevent concurrent writes from corrupting the state file. When an apply process is forcefully terminated (Ctrl+C, runner timeout), the lock remains until manually released.",
  "confidence": 0.95,
  "references": []
}
"""
