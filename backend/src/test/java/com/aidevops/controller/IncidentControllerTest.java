package com.aidevops.controller;

import com.aidevops.domain.InputType;
import com.aidevops.domain.User;
import com.aidevops.dto.request.IncidentAnalyzeRequest;
import com.aidevops.integration.ai.AiAnalyzeResponsePayload;
import com.aidevops.integration.ai.AiServiceClient;
import com.aidevops.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("engineer@aidevops.io")) {
            User user = new User("engineer@aidevops.io", "$2a$12$dummyHash", "Dev", "Engineer");
            user.setEmailVerified(true);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }

    @Test
    @WithMockUser(username = "engineer@aidevops.io", roles = {"ENGINEER"})
    @DisplayName("POST /api/v1/incidents/analyze - Successful Analysis")
    void testAnalyzeEndpoint_Success() throws Exception {
        AiAnalyzeResponsePayload mockPayload = new AiAnalyzeResponsePayload(
            "Pod terminated due to memory pressure",
            "OOMKilled exit code 137",
            "CRITICAL",
            "Pod crashing repeatedly",
            List.of("Increase memory limit"),
            List.of(new AiAnalyzeResponsePayload.CommandItemPayload("kubectl describe pod test-pod", "Check exit status", "SAFE")),
            "Process killed by OS",
            0.96,
            List.of(),
            "kubernetes",
            0,
            "mock-gemini"
        );

        when(aiServiceClient.analyze(anyString(), any(InputType.class), any()))
            .thenReturn(new AiServiceClient.AnalysisResultWrapper(mockPayload, false));

        IncidentAnalyzeRequest request = new IncidentAnalyzeRequest(
            "Back-off restarting failed container in pod test-pod exit code 137 OOMKilled",
            "Test K8s OOM",
            "KUBERNETES",
            null
        );

        mockMvc.perform(post("/api/v1/incidents/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.severity").value("CRITICAL"))
            .andExpect(jsonPath("$.inputType").value("KUBERNETES"))
            .andExpect(jsonPath("$.rootCause").value("OOMKilled exit code 137"))
            .andExpect(jsonPath("$.commands[0].command").value("kubectl describe pod test-pod"))
            .andExpect(jsonPath("$.commands[0].riskLevel").value("SAFE"));
    }

    @Test
    @DisplayName("POST /api/v1/incidents/analyze - Unauthenticated 401")
    void testAnalyzeEndpoint_Unauthenticated() throws Exception {
        IncidentAnalyzeRequest request = new IncidentAnalyzeRequest(
            "CrashLoopBackOff",
            "Unauthorized Test",
            "KUBERNETES",
            null
        );

        mockMvc.perform(post("/api/v1/incidents/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "engineer@aidevops.io", roles = {"ENGINEER"})
    @DisplayName("GET /api/v1/incidents/recent - List Recent Incidents")
    void testGetRecentIncidents() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
