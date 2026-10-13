package com.aidevops.audit;

public record SecurityAuditEvent(
    String actorEmail,
    String eventType,
    String resource,
    String status,
    String ipAddress,
    String userAgent,
    String details
) {}
