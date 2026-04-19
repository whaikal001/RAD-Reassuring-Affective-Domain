package com.SocializerAI.repository;

import com.SocializerAI.model.ReportInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportInsightRepository extends JpaRepository<ReportInsight, UUID> {
    List<ReportInsight> findByReportId(UUID reportId);
    List<ReportInsight> findByReportIdOrderByPriorityDesc(UUID reportId);
}
