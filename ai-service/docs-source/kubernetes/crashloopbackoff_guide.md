# Kubernetes CrashLoopBackOff Troubleshooting Guide

## Overview
A `CrashLoopBackOff` status means that a Pod is repeatedly crashing, restarting, and backing off exponentially. Kubernetes waits for a progressively longer period between restarts (10s, 20s, 40s... up to 5 minutes).

## Common Root Causes
1. **Application Runtime Errors**: Unhandled exceptions, syntax errors, or missing runtime configuration during container startup.
2. **Missing Environment Variables or Secrets**: The container fails to read required database passwords or API tokens from ConfigMaps or Secrets.
3. **Liveness / Readiness Probe Failures**: The probe endpoint (`/healthz` or `/actuator/health`) fails or times out before the application finishes initialization.
4. **Port Binding Conflicts or Address In Use**: The container application tries to bind to a privileged port or a port already reserved.
5. **Volume Mount Failures**: Required persistent volume claims (PVC) cannot be attached or have incorrect filesystem permissions (e.g. non-root user cannot write to `/var/data`).

## Diagnostic Commands
```bash
# 1. Check pod status and restart count
kubectl get pods -n <namespace>

# 2. View container logs (including previous crashed instance)
kubectl logs <pod-name> -n <namespace> --previous

# 3. Inspect container lifecycle events and exit code
kubectl describe pod <pod-name> -n <namespace>
```

## Remediation Steps
- **Exit Code 1**: Application exception. Inspect logs with `kubectl logs <pod> --previous`.
- **Exit Code 137**: Pod was killed by the Linux Out-Of-Memory (OOM) killer. Increase `resources.limits.memory`.
- **Exit Code 139**: Segmentation fault in underlying native binary or C libraries.
- Adjust `initialDelaySeconds` on liveness probes if the container requires extra startup time.
