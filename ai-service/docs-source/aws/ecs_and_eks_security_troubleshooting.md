# AWS ECS & EKS Cloud Security and Deployment Troubleshooting

## IAM Roles for Service Accounts (IRSA) in EKS
In Kubernetes on AWS, pods must assume IAM roles using OIDC federation rather than hardcoding static AWS access keys or using node instance profiles.

### Common Issue: AccessDenied on AWS API from Pod
```text
botocore.exceptions.ClientError: An error occurred (AccessDenied) when calling the PutObject operation: Access Denied
```

### Diagnostic Steps
1. **Check ServiceAccount Annotation**:
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: s3-uploader
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/S3UploaderRole
```
2. **Verify Pod Identity Token**: Inspect inside container:
```bash
echo $AWS_ROLE_ARN
echo $AWS_WEB_IDENTITY_TOKEN_FILE
```
3. **Verify IAM Trust Policy**:
The IAM role trust policy must specify the exact OIDC provider and service account subject:
```json
{
  "Effect": "Allow",
  "Principal": { "Federated": "arn:aws:iam::123456789012:oidc-provider/oidc.eks.us-east-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B71EXAMPLE" },
  "Action": "sts:AssumeRoleWithWebIdentity",
  "Condition": {
    "StringEquals": {
      "oidc.eks.us-east-1.amazonaws.com/id/EXAMPLED539D4633E53DE1B71EXAMPLE:sub": "system:serviceaccount:default:s3-uploader"
    }
  }
}
```
