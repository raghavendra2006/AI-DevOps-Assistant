package com.aidevops.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "remediation_steps")
public class RemediationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @JsonIgnore
    private Incident incident;

    @Column(name = "step_description", nullable = false, columnDefinition = "TEXT")
    private String stepDescription;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder = 0;

    public RemediationStep() {}

    public RemediationStep(Incident incident, String stepDescription, Integer stepOrder) {
        this.incident = incident;
        this.stepDescription = stepDescription;
        this.stepOrder = stepOrder;
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

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }
}
