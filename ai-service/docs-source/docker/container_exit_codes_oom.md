# Docker Container Exit Codes and Memory Optimization

## Common Container Exit Codes
When a Docker container abruptly stops, its exit status indicates the failure mode:

| Exit Code | Signal / Name | Root Cause & Diagnostic |
| :--- | :--- | :--- |
| **0** | Success | Container finished its main process cleanly without error. |
| **1** | Application Error | General unhandled runtime error (e.g. fatal JavaScript error, Python traceback, missing dependency). |
| **127** | Command Not Found | The binary or shell script specified in `ENTRYPOINT` or `CMD` does not exist in `$PATH`. |
| **137** | SIGKILL (128 + 9) | The kernel killed the container process. Almost always caused by **OOM (Out Of Memory)** killer. |
| **139** | SIGSEGV (128 + 11) | Segmentation fault: memory access violation in native code or corrupted shared library. |
| **143** | SIGTERM (128 + 15) | Graceful shutdown signal received from Docker daemon or orchestrator during rolling update. |

## Diagnosing OOMKilled (Exit Code 137)
```bash
# Check if container was terminated by OOM
docker inspect <container-id> --format='{{.State.OOMKilled}}'

# Inspect memory stats and limits
docker stats <container-id> --no-stream
```

### Remediation
1. Increase container memory limit: `docker run -m 2g ...` or update Docker Compose `deploy.resources.limits.memory: 2G`.
2. For Java / JVM applications: Add `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` to respect container cgroups.
