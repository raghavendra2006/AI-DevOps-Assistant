package com.aidevops.audit;

import com.aidevops.domain.AuditLog;
import com.aidevops.dto.response.AuditLogResponse;
import com.aidevops.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void recordEvent(SecurityAuditEvent event) {
        AuditLog log = new AuditLog(
            event.actorEmail(),
            event.eventType(),
            event.resource(),
            event.status(),
            event.ipAddress(),
            event.userAgent(),
            event.details()
        );
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc().stream()
            .map(AuditLogResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsPaged(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
            .map(AuditLogResponse::fromEntity);
    }
}
