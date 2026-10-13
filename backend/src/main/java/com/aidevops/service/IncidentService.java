package com.aidevops.service;

import com.aidevops.domain.*;
import com.aidevops.dto.request.IncidentAnalyzeRequest;
import com.aidevops.dto.response.*;
import com.aidevops.exception.ResourceNotFoundException;
import com.aidevops.integration.ai.AiAnalyzeResponsePayload;
import com.aidevops.integration.ai.AiServiceClient;
import com.aidevops.repository.IncidentLogRepository;
import com.aidevops.repository.IncidentRepository;
import com.aidevops.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentLogRepository incidentLogRepository;
    private final UserRepository userRepository;
    private final LogPreprocessorService preprocessorService;
    private final AiServiceClient aiServiceClient;

    public IncidentService(
        IncidentRepository incidentRepository,
        IncidentLogRepository incidentLogRepository,
        UserRepository userRepository,
        LogPreprocessorService preprocessorService,
        AiServiceClient aiServiceClient
    ) {
        this.incidentRepository = incidentRepository;
        this.incidentLogRepository = incidentLogRepository;
        this.userRepository = userRepository;
        this.preprocessorService = preprocessorService;
        this.aiServiceClient = aiServiceClient;
    }

    @Transactional
    public IncidentDetailResponse analyzeIncident(IncidentAnalyzeRequest request, String userEmail, String logSource) {
        User user = null;
        if (userEmail != null && !userEmail.isBlank()) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        // 1. Preprocess & redact log
        LogPreprocessorService.PreprocessedLog preprocessed = preprocessorService.preprocess(
            request.content(),
            request.inputType()
        );

        // 2. Generate title if not provided
        String title = request.title();
        if (title == null || title.isBlank()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            title = String.format("%s Diagnostic Triage (%s)", preprocessed.inputType().name(), timestamp);
        }

        // 3. Create initial incident & log entity
        Incident incident = new Incident(title, preprocessed.inputType(), user);
        incident = incidentRepository.save(incident);

        IncidentLog incidentLog = new IncidentLog(
            incident,
            preprocessed.rawLog(),
            preprocessed.sanitizedLog(),
            logSource != null ? logSource : "manual_paste",
            preprocessed.sizeBytes(),
            preprocessed.redactedCount()
        );
        incidentLogRepository.save(incidentLog);
        incident.addLog(incidentLog);

        // 4. Call AI microservice (with circuit breaker & fallback)
        AiServiceClient.AnalysisResultWrapper resultWrapper = aiServiceClient.analyze(
            preprocessed.sanitizedLog(),
            preprocessed.inputType(),
            request.context()
        );
        AiAnalyzeResponsePayload aiResponse = resultWrapper.payload();

        // 5. Update incident fields
        incident.setSummary(aiResponse.summary());
        incident.setRootCause(aiResponse.rootCause());
        incident.setImpact(aiResponse.impact());
        incident.setExplanation(aiResponse.explanation());
        incident.setConfidence(aiResponse.confidence());
        incident.setModelUsed(aiResponse.providerUsed());
        incident.setIsFallback(resultWrapper.isFallback());
        incident.setStatus(IncidentStatus.ANALYZED);

        try {
            incident.setSeverity(Severity.valueOf(aiResponse.severity().toUpperCase()));
        } catch (Exception e) {
            incident.setSeverity(Severity.HIGH);
        }

        // 6. Persist remediation steps
        if (aiResponse.fixSteps() != null) {
            for (int i = 0; i < aiResponse.fixSteps().size(); i++) {
                incident.addStep(new RemediationStep(incident, aiResponse.fixSteps().get(i), i + 1));
            }
        }

        // 7. Persist remediation commands
        if (aiResponse.commands() != null) {
            for (int i = 0; i < aiResponse.commands().size(); i++) {
                AiAnalyzeResponsePayload.CommandItemPayload cmd = aiResponse.commands().get(i);
                RiskLevel risk = RiskLevel.SAFE;
                if (cmd.riskLevel() != null) {
                    try {
                        risk = RiskLevel.valueOf(cmd.riskLevel().toUpperCase());
                    } catch (Exception ignored) {}
                }
                incident.addCommand(new RemediationCommand(incident, cmd.command(), cmd.description(), risk, i + 1));
            }
        }

        // 8. Persist references
        if (aiResponse.references() != null) {
            for (AiAnalyzeResponsePayload.ReferenceItemPayload ref : aiResponse.references()) {
                incident.addReference(new AnalysisReference(incident, ref.title(), ref.source(), ref.docId(), ref.url()));
            }
        }

        incident = incidentRepository.save(incident);
        log.info("Incident ID #{} successfully analyzed and persisted (isFallback={})", incident.getId(), resultWrapper.isFallback());

        return mapToDetailResponse(incident, incidentLog);
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getIncidentById(Long id, String userEmail, boolean isAdmin) {
        Incident incident = incidentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + id));

        IncidentLog log = incident.getLogs().isEmpty() ? null : incident.getLogs().get(0);
        return mapToDetailResponse(incident, log);
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> getUserIncidents(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        return incidentRepository.findByUser(user, pageable)
            .map(this::mapToSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryResponse> getRecentIncidents(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return List.of();
        }
        return incidentRepository.findTop10ByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(this::mapToSummaryResponse)
            .toList();
    }

    private IncidentDetailResponse mapToDetailResponse(Incident incident, IncidentLog log) {
        List<String> fixSteps = incident.getSteps().stream()
            .map(RemediationStep::getStepDescription)
            .toList();

        List<RemediationCommandDto> commands = incident.getCommands().stream()
            .map(c -> new RemediationCommandDto(c.getId(), c.getCommand(), c.getDescription(), c.getRiskLevel(), c.getStepOrder()))
            .toList();

        List<AnalysisReferenceDto> references = incident.getReferences().stream()
            .map(r -> new AnalysisReferenceDto(r.getId(), r.getTitle(), r.getSource(), r.getDocId(), r.getUrl()))
            .toList();

        String sanitizedLog = log != null ? log.getSanitizedContent() : "";
        Integer redactedCount = log != null ? log.getRedactedTokensCount() : 0;

        return new IncidentDetailResponse(
            incident.getId(),
            incident.getTitle(),
            incident.getStatus(),
            incident.getSeverity(),
            incident.getInputType(),
            incident.getSummary(),
            incident.getRootCause(),
            incident.getImpact(),
            incident.getExplanation(),
            incident.getConfidence(),
            incident.getModelUsed(),
            incident.getIsFallback(),
            sanitizedLog,
            redactedCount,
            fixSteps,
            commands,
            references,
            incident.getCreatedAt(),
            incident.getUpdatedAt()
        );
    }

    private IncidentSummaryResponse mapToSummaryResponse(Incident incident) {
        return new IncidentSummaryResponse(
            incident.getId(),
            incident.getTitle(),
            incident.getStatus(),
            incident.getSeverity(),
            incident.getInputType(),
            incident.getSummary(),
            incident.getConfidence(),
            incident.getIsFallback(),
            incident.getCreatedAt()
        );
    }
}
