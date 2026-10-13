# Terraform State Locking & Drift Remediation

## State Locking with AWS S3 and DynamoDB
Terraform uses DynamoDB to acquire a distributed state lock during `plan` and `apply` operations. This prevents multiple concurrent executions from corrupting the remote state file stored in S3.

### Error: Error acquiring the state lock
```text
Error: Error acquiring the state lock: 2 errors occurred:
* ConditionalCheckFailedException: The conditional request failed
Lock Info:
  ID: 15e8a5b2-3b4c-4e89-b0c1-2a3b4c5d6e7f
  Path: my-infra-bucket/terraform.tfstate
  Operation: OperationTypeApply
  Who: devops-ci@runner-01
```

### Remediation
1. **Verify Concurrency**: Check if another CI/CD pipeline or engineer is legitimately running `terraform apply`.
2. **Clear Orphaned Lock**: If the previous job was forcibly aborted or timed out without releasing the lock:
```bash
terraform force-unlock 15e8a5b2-3b4c-4e89-b0c1-2a3b4c5d6e7f
```
3. **Validate State Integrity**: Immediately run `terraform refresh` and `terraform plan` to confirm the state is healthy.

## Infrastructure Drift Detection
Drift occurs when resources are modified out-of-band via the cloud console or API without updating the Terraform configuration.
- Run `terraform plan -detailed-exitcode` in a nightly cron pipeline.
- Exit code 0: No changes.
- Exit code 2: Drift detected. Alert the DevOps on-call team.
