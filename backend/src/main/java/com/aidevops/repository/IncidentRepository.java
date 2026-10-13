package com.aidevops.repository;

import com.aidevops.domain.Incident;
import com.aidevops.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Page<Incident> findByUser(User user, Pageable pageable);
    List<Incident> findTop10ByUserOrderByCreatedAtDesc(User user);
    List<Incident> findTop10ByOrderByCreatedAtDesc();
}
