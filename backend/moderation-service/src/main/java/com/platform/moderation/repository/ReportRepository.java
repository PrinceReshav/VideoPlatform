package com.platform.moderation.repository;

import com.platform.moderation.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByReporterId(UUID reporterId, Pageable pageable);

    List<Report> findByTargetTypeAndTargetId(Report.TargetType targetType, UUID targetId);

    Page<Report> findByStatus(Report.Status status, Pageable pageable);

    Page<Report> findByTargetTypeAndStatus(Report.TargetType targetType, Report.Status status, Pageable pageable);

    long countByStatus(Report.Status status);

    @Query("SELECT r FROM Report r WHERE r.status = 'PENDING' ORDER BY r.priority DESC, r.createdAt ASC")
    List<Report> findPendingReports();
}