package com.radai.repository;

import com.radai.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByUserId(UUID userId);
    List<Report> findByUserIdOrderByGeneratedAtDesc(UUID userId);
}

