package com.aidevops.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditListener.class);

    private final AuditLogService auditLogService;

    public SecurityAuditListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Async
    @EventListener
    public void onSecurityAuditEvent(SecurityAuditEvent event) {
        try {
            log.info("Recording security audit event: [{}] by [{}] - status: {}",
                event.eventType(), event.actorEmail(), event.status());
            auditLogService.recordEvent(event);
        } catch (Exception ex) {
            log.error("Failed to persist security audit event: {}", event, ex);
        }
    }
}
