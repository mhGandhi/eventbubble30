package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.controller.ModerationController;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.tickets.Report;
import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import com.lennadi.eventbubble30.features.db.repository.tickets.TicketRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
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

        return ticketRepository.save(report);
    }

    @PreAuthorize("@authz.hasRole('ADMIN') or @authz.hasRole('MODERATOR')")
    public Page<Ticket> searchTickets(ModerationController.TicketSearch s, int page, int size) {
        Specification<Ticket> spec =
                TicketRepository.TicketSpecs.messageSearch(s.q())
                .and(TicketRepository.TicketSpecs.closed(s.closed()))
                .and(TicketRepository.TicketSpecs.escalate(s.escalate()))
                .and(TicketRepository.TicketSpecs.createdBy(s.createdBy()))
                .and(TicketRepository.TicketSpecs.assignedTo(s.assignedTo()))
                .and(TicketRepository.TicketSpecs.createdBetween(s.createdFrom(), s.createdTo()))
                .and(TicketRepository.TicketSpecs.modifiedBetween(s.modifiedFrom(), s.modifiedTo()))
                .and(TicketRepository.TicketSpecs.typeEquals(s.type())); // optional

        String sortField = switch(s.orderBy()){
            case creationDate -> "creationDate";
            case modificationDate ->  "modificationDate";
        };

        Sort sort = Sort.by(
                s.orderDir() == TicketRepository.OrderDir.asc ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortField
        );

        return ticketRepository.findAll(spec, PageRequest.of(page, size, sort));
    }

    @PreAuthorize("@authz.hasRole('ADMIN') or @authz.hasRole('MODERATOR') or @authz.isTicketAuthor(#externalId)")
    public Ticket getByExternalIdOrThrow(String externalId) {
        return ticketRepository.findByExternalIdIgnoreCase(externalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ticket not found"
                ));
    }

    @PreAuthorize("@authz.hasRole('ADMIN') or @authz.hasRole('MODERATOR')")
    @Transactional
    public Ticket patchTicket(String externalId, ModerationController.PatchTicketRequest req) {
        Ticket t = ticketRepository.findByExternalIdIgnoreCase(externalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (req.closed() != null) t.setClosed(req.closed());
        if (req.escalate() != null) t.setEscalate(req.escalate());
        if (req.comment() != null) t.setComment(req.comment());

        if (req.assignedToUserId() != null) {
            if (req.assignedToUserId().isBlank()) {
                // allow unassign
                t.setAssignedTo(null);
            } else {
                var user = benutzerService.requireUser(req.assignedToUserId());
                t.setAssignedTo(user);
            }
        }
        return t;
    }

    @PreAuthorize("@authz.hasRole('ADMIN') or @authz.hasRole('MODERATOR')")
    @Transactional
    public Ticket patchReport(String externalId, ModerationController.PatchReportRequest req) {
        Ticket t = ticketRepository.findByExternalIdIgnoreCase(externalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (!(t instanceof Report r)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket is not a report");
        }

        if (req.outcome() != null) r.setOutcome(req.outcome());

        return r;
    }
}

