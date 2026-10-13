package com.aidevops.repository;

import com.aidevops.domain.Incident;
import com.aidevops.domain.IncidentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentLogRepository extends JpaRepository<IncidentLog, Long> {
    List<IncidentLog> findByIncident(Incident incident);
}
