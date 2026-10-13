package com.aidevops.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Severity severity = Severity.HIGH;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 50)
    private InputType inputType = InputType.GENERIC;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String impact;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column
    private Double confidence = 0.0;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "is_fallback", nullable = false)
    private Boolean isFallback = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentLog> logs = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<RemediationCommand> commands = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<RemediationStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisReference> references = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Incident() {}

    public Incident(String title, InputType inputType, User user) {
        this.title = title;
        this.inputType = inputType;
        this.user = user;
        this.status = IncidentStatus.ANALYZING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addCommand(RemediationCommand cmd) {
        cmd.setIncident(this);
        this.commands.add(cmd);
    }

    public void addStep(RemediationStep step) {
        step.setIncident(this);
        this.steps.add(step);
    }

    public void addReference(AnalysisReference ref) {
        ref.setIncident(this);
        this.references.add(ref);
    }

    public void addLog(IncidentLog log) {
        log.setIncident(this);
        this.logs.add(log);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public Boolean getIsFallback() {
        return isFallback;
    }

    public void setIsFallback(Boolean isFallback) {
        this.isFallback = isFallback;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<IncidentLog> getLogs() {
        return logs;
    }

    public void setLogs(List<IncidentLog> logs) {
        this.logs = logs;
    }

    public List<RemediationCommand> getCommands() {
        return commands;
    }

    public void setCommands(List<RemediationCommand> commands) {
        this.commands = commands;
    }

    public List<RemediationStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RemediationStep> steps) {
        this.steps = steps;
    }

    public List<AnalysisReference> getReferences() {
        return references;
    }

    public void setReferences(List<AnalysisReference> references) {
        this.references = references;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
