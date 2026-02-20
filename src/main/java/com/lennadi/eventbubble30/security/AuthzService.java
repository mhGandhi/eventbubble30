package com.lennadi.eventbubble30.security;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import com.lennadi.eventbubble30.features.db.entities.tickets.Ticket;
import com.lennadi.eventbubble30.features.db.repository.tickets.TicketRepository;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.features.service.TicketService;
import com.lennadi.eventbubble30.features.service.VeranstaltungService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("authz")
@RequiredArgsConstructor
public class AuthzService {
    private final VeranstaltungService veranstaltungService;
    private final BenutzerService benutzerService;
    private final TicketRepository ticketRepository;

    public boolean isEventOwner(String eventExtId) {
        Benutzer current = benutzerService.getCurrentUserOrNull();
        if (current == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Veranstaltung v = veranstaltungService.getVeranstaltungById(eventExtId);
        if (v == null) {
            throw new AccessDeniedException("Event " + eventExtId + " does not exist");
        }

        if (!v.getBesitzer().equals(current)) {
            throw new AccessDeniedException(
                    "User " + current.getId() + " is not the owner of event " + eventExtId
            );
        }

        return true;
    }

    public boolean isTicketAuthor(String ticketExtId) {
        Benutzer current = benutzerService.getCurrentUserOrNull();
        if(current == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        Ticket t = ticketRepository.findByExternalIdIgnoreCase(ticketExtId).orElse(null);
        if(t==null) {
            throw new AccessDeniedException("Ticket " + ticketExtId + " does not exist");
        }

        if(!t.getCreatedBy().getId().equals(current.getId())) {
            throw new AccessDeniedException("User " + current.getId() + " is not the owner of ticket " + ticketExtId);
        }

        return true;
    }

    public boolean isSelf(Long userId) {
        var current = benutzerService.getCurrentUserOrNull();
        if (current == null)
            throw new AccessDeniedException("Not authenticated");

        if (!current.getId().equals(userId))
            throw new AccessDeniedException(
                    "User " + current.getId() + " cannot access resource of user " + userId
            );

        return true;
    }

    public boolean isSelf(String extUserId) {
        var current = benutzerService.getCurrentUserOrNull();
        if (current == null)
            throw new AccessDeniedException("Not authenticated");

        if (!current.getExternalId().equals(extUserId))
            throw new AccessDeniedException(
                    "User " + current.getExternalId() + " cannot access resource of user " + extUserId
            );

        return true;
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities() == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        boolean ok = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));

        if (!ok) {
            throw new AccessDeniedException(
                    "User lacks required role: ROLE_" + role
            );
        }

        return true;
    }

    public boolean isAuthenticated(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities() == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        return true;
    }

    public boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        boolean ok = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));

        if (!ok) {
            throw new AccessDeniedException(
                    "User lacks required authority: " + authority
            );
        }

        return true;
    }
}
