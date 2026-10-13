package com.aidevops.dto.response;

import com.aidevops.domain.AuditLog;
import java.time.Instant;

public record AuditLogResponse(
    Long id,
    String actorEmail,
    String eventType,
    String resource,
    String status,
    String ipAddress,
    String userAgent,
    String details,
    Instant timestamp
) {
    public static AuditLogResponse fromEntity(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getActorEmail(),
            log.getEventType(),
            log.getResource(),
            log.getStatus(),
            log.getIpAddress(),
            log.getUserAgent(),
            log.getDetails(),
            log.getTimestamp()
        );
    }
}
