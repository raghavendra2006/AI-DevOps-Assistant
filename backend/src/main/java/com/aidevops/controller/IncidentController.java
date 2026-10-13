package com.aidevops.controller;

import com.aidevops.dto.request.IncidentAnalyzeRequest;
import com.aidevops.dto.response.IncidentDetailResponse;
import com.aidevops.dto.response.IncidentSummaryResponse;
import com.aidevops.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "AI Incident Analysis", description = "Endpoints for analyzing DevOps logs, triaging failures, and retrieving incident history")
@SecurityRequirement(name = "BearerAuth")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze incident log (JSON)", description = "Preprocesses, sanitizes, and analyzes pasted log text using RAG and LLM")
    public ResponseEntity<IncidentDetailResponse> analyzeLog(
        @Valid @RequestBody IncidentAnalyzeRequest request,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        String email = currentUser != null ? currentUser.getUsername() : null;
        IncidentDetailResponse response = incidentService.analyzeIncident(request, email, "manual_paste");
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/analyze/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze incident log file (Upload)", description = "Uploads a log file (.log, .txt, .json, .yaml), sanitizes, and analyzes it")
    public ResponseEntity<IncidentDetailResponse> analyzeFile(
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "inputType", required = false) String inputType,
        @AuthenticationPrincipal UserDetails currentUser
    ) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String fileTitle = title != null && !title.isBlank() ? title : file.getOriginalFilename();
        IncidentAnalyzeRequest request = new IncidentAnalyzeRequest(content, fileTitle, inputType, null);

        String email = currentUser != null ? currentUser.getUsername() : null;
        IncidentDetailResponse response = incidentService.analyzeIncident(request, email, file.getOriginalFilename());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List user incidents", description = "Retrieves paginated incident triage history for the authenticated user")
    public ResponseEntity<Page<IncidentSummaryResponse>> getUserIncidents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        String email = currentUser != null ? currentUser.getUsername() : null;
        Page<IncidentSummaryResponse> incidents = incidentService.getUserIncidents(
            email,
            PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent user incidents", description = "Retrieves up to 10 recent incidents for dashboard overview")
    public ResponseEntity<List<IncidentSummaryResponse>> getRecentIncidents(
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        String email = currentUser != null ? currentUser.getUsername() : null;
        List<IncidentSummaryResponse> recent = incidentService.getRecentIncidents(email);
        return ResponseEntity.ok(recent);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident details by ID", description = "Retrieves comprehensive diagnostic report, commands, steps, and references")
    public ResponseEntity<IncidentDetailResponse> getIncidentById(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        String email = currentUser != null ? currentUser.getUsername() : null;
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        IncidentDetailResponse incident = incidentService.getIncidentById(id, email, isAdmin);
        return ResponseEntity.ok(incident);
    }
}
