package com.lennadi.eventbubble30.features.db.repository.tickets;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.tickets.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
    Page<Report> findByEntityTypeAndResourceId(EntityType entityType, String resourceId, Pageable pageable);
    Page<Report> findByReason(Report.Reason reason, Pageable pageable);
}
