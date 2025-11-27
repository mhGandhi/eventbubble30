package com.lennadi.eventbubble30.logging;

import com.lennadi.eventbubble30.entities.Benutzer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Set;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findAll(Pageable pageable);
    Page<AuditLog> findByAction(AuditLog.Action action, Pageable pageable);
    Page<AuditLog> findByActionIn(Set<AuditLog.Action> actions, Pageable pageable);
    Page<AuditLog> findByActionNotIn(Set<AuditLog.Action> excluded, Pageable pageable);
    Page<AuditLog> findByBenutzer(Benutzer benutzer, Pageable pageable);
    Page<AuditLog> findByResourceTypeAndResourceId(String type, Long id, Pageable pageable);
}
