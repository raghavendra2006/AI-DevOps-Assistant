package com.aidevops.controller;

import com.aidevops.audit.AuditLogService;
import com.aidevops.dto.response.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Security Audit Logs", description = "Admin endpoints for inspecting security audit events and authentication history")
@SecurityRequirement(name = "BearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List audit logs (Admin only)", description = "Retrieves paginated audit log entries with timestamps, event types, actors, and IP addresses")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogsPaged(PageRequest.of(page, size)));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get recent 50 audit logs (Admin only)")
    public ResponseEntity<List<AuditLogResponse>> getRecentAuditLogs() {
        return ResponseEntity.ok(auditLogService.getRecentAuditLogs());
    }
}
