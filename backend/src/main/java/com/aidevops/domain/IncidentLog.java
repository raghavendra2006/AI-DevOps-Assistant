package com.aidevops.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident_logs")
public class IncidentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @JsonIgnore
    private Incident incident;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "sanitized_content", columnDefinition = "TEXT")
    private String sanitizedContent;

    @Column(name = "log_source", length = 100)
    private String logSource = "manual_paste";

    @Column(name = "size_bytes")
    private Long sizeBytes = 0L;

    @Column(name = "redacted_tokens_count")
    private Integer redactedTokensCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public IncidentLog() {}

    public IncidentLog(Incident incident, String rawContent, String sanitizedContent, String logSource, Long sizeBytes, Integer redactedTokensCount) {
        this.incident = incident;
        this.rawContent = rawContent;
        this.sanitizedContent = sanitizedContent;
        this.logSource = logSource;
        this.sizeBytes = sizeBytes;
        this.redactedTokensCount = redactedTokensCount;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getSanitizedContent() {
        return sanitizedContent;
    }

    public void setSanitizedContent(String sanitizedContent) {
        this.sanitizedContent = sanitizedContent;
    }

    public String getLogSource() {
        return logSource;
    }

    public void setLogSource(String logSource) {
        this.logSource = logSource;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getRedactedTokensCount() {
        return redactedTokensCount;
    }

    public void setRedactedTokensCount(Integer redactedTokensCount) {
        this.redactedTokensCount = redactedTokensCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
