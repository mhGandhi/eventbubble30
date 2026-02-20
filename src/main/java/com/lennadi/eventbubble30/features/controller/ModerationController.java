package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.tickets.Report;
import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import com.lennadi.eventbubble30.features.db.repository.tickets.TicketRepository;
import com.lennadi.eventbubble30.features.service.TicketService;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("api/moderation")
@RequiredArgsConstructor
public class ModerationController {
    private final TicketService ticketService;

    public record CreateTicketRequest(
            @NotBlank
            @Size(max = 2000)
            String message
    ){}

    public record CreateReportRequest(
            @NotNull
            EntityType entityType,

            @NotBlank
            @Size(max = 128)
            String resourceId,//public id

            @NotNull
            Report.Reason reason,

            @Size(max = 1000)
            String reasonText,

            @Size(max = 2000)
            String message // optional extra text by reporter
    ) {}

    public record PatchTicketRequest(
            Boolean closed,
            Boolean escalate,
            String comment,
            String assignedToUserId // externalId of Benutzer (or use Long id if you prefer)
    ) {}
    public record PatchReportRequest(
            Report.Outcome outcome
    ) {}


    public record TicketSearch(
            String q,

            Boolean closed,
            Boolean escalate,

            String createdBy,   // user externalId
            String assignedTo,  // user externalId

            Instant createdFrom,
            Instant createdTo,

            Instant modifiedFrom,
            Instant modifiedTo,

            String type,        // discriminator value, e.g. "REPORT"

            TicketRepository.OrderBy orderBy,
            TicketRepository.OrderDir orderDir
    ) {}
    private static final int MAX_PAGE_SIZE = 200;



    @Audit(action = AuditLog.Action.CREATE, resourceType = EntityType.TICKET, resourceIdExpression = "#result.body.id")
    @PostMapping("/tickets")
    public ResponseEntity<Ticket.DTO> createTicket(@Valid @RequestBody CreateTicketRequest req) {
        Ticket neu = ticketService.createTicket(req);

        return ResponseEntity
                .created(URI.create("/api/moderation/tickets/" + neu.getExternalId()))
                .body(neu.toDTO());
    }

    @Audit(action = AuditLog.Action.CREATE, resourceType = EntityType.TICKET, resourceIdExpression = "#result.body.id")
    @PostMapping("/reports")
    public ResponseEntity<Ticket.DTO> createReport(@Valid @RequestBody CreateReportRequest req) {
        Report neu = ticketService.createReport(req);

        return ResponseEntity
                .created(URI.create("/api/moderation/tickets/" + neu.getExternalId()))
                .body(neu.toDTO());
    }

    @GetMapping("/tickets/{id}")
    public Ticket.DTO getTicket(@PathVariable("id") String externalId) {
        return ticketService.getByExternalIdOrThrow(externalId).toDTO();
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = EntityType.TICKET, resourceIdExpression = "#result.body.id")
    @PatchMapping("/tickets/{id}")
    public ResponseEntity<Ticket.DTO> patchTicket(
            @PathVariable("id") String id,
            @RequestBody PatchTicketRequest req
    ) {
        Ticket updated = ticketService.patchTicket(id, req);
        return ResponseEntity.ok(updated.toDTO());
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = EntityType.TICKET, resourceIdExpression = "#result.body.id")
    @PatchMapping("/reports/{id}")
    public ResponseEntity<Ticket.DTO> patchReport(
            @PathVariable("id") String id,
            @RequestBody PatchReportRequest req
    ) {
        Ticket updated = ticketService.patchReport(id, req);
        return ResponseEntity.ok(updated.toDTO());
    }

    @GetMapping({"/tickets", "/tickets/"})
    public Page<Ticket.DTO> listTickets(
            @RequestParam(required = false) String q,

            @RequestParam(required = false) Boolean closed,
            @RequestParam(required = false) Boolean escalate,

            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String assignedTo,

            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,

            @RequestParam(required = false) Instant modifiedFrom,
            @RequestParam(required = false) Instant modifiedTo,

            @RequestParam(required = false) String type,

            @RequestParam(defaultValue = "creationDate") TicketRepository.OrderBy orderBy,
            @RequestParam(defaultValue = "desc") TicketRepository.OrderDir orderDir,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) badRequest("Page number is negative");
        if (size < 1) badRequest("Page Size must be >= 1");
        if (size > MAX_PAGE_SIZE) badRequest("Page Size exceeds max of " + MAX_PAGE_SIZE);

        TicketSearch search = parseSearch(
                q,
                closed, escalate,
                createdBy, assignedTo,
                createdFrom, createdTo,
                modifiedFrom, modifiedTo,
                type,
                orderBy, orderDir
        );

        return ticketService
                .searchTickets(search, page, size)
                .map(Ticket::toDTO);
    }

    private TicketSearch parseSearch(
            String q,
            Boolean closed,
            Boolean escalate,
            String createdBy,
            String assignedTo,
            Instant createdFrom,
            Instant createdTo,
            Instant modifiedFrom,
            Instant modifiedTo,
            String type,
            TicketRepository.OrderBy orderBy,
            TicketRepository.OrderDir orderDir
    ) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo))
            badRequest("'createdFrom' must be <= 'createdTo'");

        if (modifiedFrom != null && modifiedTo != null && modifiedFrom.isAfter(modifiedTo))
            badRequest("'modifiedFrom' must be <= 'modifiedTo'");

        return new TicketSearch(
                q,
                closed, escalate,
                createdBy, assignedTo,
                createdFrom, createdTo,
                modifiedFrom, modifiedTo,
                type,
                orderBy, orderDir
        );
    }

    private static void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
