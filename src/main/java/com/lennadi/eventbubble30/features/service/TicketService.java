package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.db.repository.tickets.ReportRepository;
import com.lennadi.eventbubble30.features.db.repository.tickets.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final ReportRepository reportRepository;
}
