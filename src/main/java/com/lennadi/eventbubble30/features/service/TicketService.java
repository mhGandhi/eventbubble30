package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.controller.ModerationController;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.tickets.Report;
import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import com.lennadi.eventbubble30.features.db.repository.tickets.ReportRepository;
import com.lennadi.eventbubble30.features.db.repository.tickets.TicketRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final ReportRepository reportRepository;
    private final BenutzerService benutzerService;

    @PreAuthorize("@authz.isAuthenticated()")
    public Ticket createTicket(ModerationController.@Valid CreateTicketRequest req) {
        Benutzer me = benutzerService.getCurrentUserOrNull();

        Ticket ticket = new Ticket(req.message(), me);
        return ticketRepository.save(ticket);
    }

    @PreAuthorize("@authz.isAuthenticated()")
    public Report createReport(ModerationController.@Valid CreateReportRequest req) {
        Benutzer me = benutzerService.getCurrentUserOrNull();

        // enforce "reasonText required if OTHER"
        if (req.reason() == Report.Reason.OTHER) {
            if (req.reasonText() == null || req.reasonText().isBlank()) {
                throw new IllegalArgumentException("reasonText is required when reason=OTHER");
            }
        }

        Report report = new Report(
                req.message(),
                me,
                req.entityType(),
                req.resourceId(),
                req.reason(),
                req.reasonText()
        );

        return reportRepository.save(report);
    }
}

