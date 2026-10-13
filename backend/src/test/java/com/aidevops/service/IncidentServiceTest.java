package com.aidevops.service;

import com.aidevops.domain.*;
import com.aidevops.dto.request.IncidentAnalyzeRequest;
import com.aidevops.dto.response.IncidentDetailResponse;
import com.aidevops.integration.ai.AiAnalyzeResponsePayload;
import com.aidevops.integration.ai.AiServiceClient;
import com.aidevops.repository.IncidentLogRepository;
import com.aidevops.repository.IncidentRepository;
import com.aidevops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentLogRepository incidentLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogPreprocessorService preprocessorService;

    @Mock
    private AiServiceClient aiServiceClient;

    @InjectMocks
    private IncidentService incidentService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("engineer@aidevops.io", "hash", "Dev", "Engineer");
        user.setId(10L);
    }

    @Test
    void testAnalyzeIncident_Success() {
        String logContent = "Warning Back-off restarting failed container in pod auth-svc-789f exit code 137";
        IncidentAnalyzeRequest request = new IncidentAnalyzeRequest(logContent, "K8s Crash Test", "KUBERNETES", null);

        LogPreprocessorService.PreprocessedLog preprocessed = new LogPreprocessorService.PreprocessedLog(
            logContent,
            logContent,
            InputType.KUBERNETES,
            0,
            logContent.length()
        );
        when(preprocessorService.preprocess(anyString(), any())).thenReturn(preprocessed);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        Incident incident = new Incident("K8s Crash Test", InputType.KUBERNETES, user);
        incident.setId(100L);
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        AiAnalyzeResponsePayload aiPayload = new AiAnalyzeResponsePayload(
            "Container killed due to OOM limit",
            "Linux SIGKILL 137",
            "CRITICAL",
            "Auth service pod restart loop",
            List.of("Increase memory limit"),
            List.of(new AiAnalyzeResponsePayload.CommandItemPayload("kubectl describe pod auth-svc-789f", "Inspect termination status", "SAFE")),
            "OOM killer sent SIGKILL",
            0.95,
            List.of(new AiAnalyzeResponsePayload.ReferenceItemPayload("K8s Runbook", "k8s_docs", "kb-1", "https://k8s.io")),
            "kubernetes",
            0,
            "gemini-1.5-pro"
        );
        when(aiServiceClient.analyze(anyString(), any(), any()))
            .thenReturn(new AiServiceClient.AnalysisResultWrapper(aiPayload, false));

        IncidentDetailResponse response = incidentService.analyzeIncident(request, user.getEmail(), "manual_paste");

        assertNotNull(response);
        assertEquals("CRITICAL", response.severity().name());
        assertEquals(InputType.KUBERNETES, response.inputType());
        assertFalse(response.isFallback());
        assertEquals(1, response.fixSteps().size());
        assertEquals(1, response.commands().size());
        assertEquals(RiskLevel.SAFE, response.commands().get(0).riskLevel());
        verify(incidentRepository, atLeastOnce()).save(any(Incident.class));
    }
}
