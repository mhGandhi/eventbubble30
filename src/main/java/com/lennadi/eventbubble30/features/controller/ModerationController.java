package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/moderation")
@RequiredArgsConstructor
public class ModerationController {
    private final TicketService ticketService;


}
