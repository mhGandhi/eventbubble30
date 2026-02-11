package com.lennadi.eventbubble30.features.db.repository.tickets;

import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    Page<Ticket> findByClosedFalse(Pageable pageable);
    Page<Ticket> findByEscalateTrue(Pageable pageable);
    Page<Ticket> findByCreatedBy_Id(Long userId, Pageable pageable);
    Page<Ticket> findByAssignedTo_Id(Long userId, Pageable pageable);
}

