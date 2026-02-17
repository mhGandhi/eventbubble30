package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.tickets.Report;
import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import com.lennadi.eventbubble30.features.service.TicketService;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
            String resourceId,

            @NotNull
            Report.Reason reason,

            @Size(max = 1000)
            String reasonText,

            @Size(max = 2000)
            String message // optional extra text by reporter
    ) {}

    @Audit(action = AuditLog.Action.CREATE, resourceType = EntityType.TICKET, resourceIdExpression = "#result.body.id")
    @PostMapping("/tickets")
    public ResponseEntity<Ticket.TicketDTO> createTicket(@Valid @RequestBody CreateTicketRequest req) {
        Ticket neu = ticketService.createTicket(req);

        return ResponseEntity
                .created(URI.create("/api/moderation/tickets/" + neu.getId()))
                .body(neu.toTicketDTO());
    }

    @Audit(action = AuditLog.Action.CREATE, resourceType = EntityType.TICKET, resourceIdExpression = "#result.body.id")
    @PostMapping("/reports")
    public ResponseEntity<Report.ReportDTO> createReport(@Valid @RequestBody CreateReportRequest req) {
        Report neu = ticketService.createReport(req);

        return ResponseEntity
                .created(URI.create("/api/moderation/reports/" + neu.getId()))
                .body(neu.toDTO());
    }


}
