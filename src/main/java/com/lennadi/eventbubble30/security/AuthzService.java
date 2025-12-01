package com.lennadi.eventbubble30.security;

import com.lennadi.eventbubble30.features.entities.Benutzer;
import com.lennadi.eventbubble30.features.entities.Veranstaltung;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.features.service.VeranstaltungService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("authz")
@RequiredArgsConstructor
public class AuthzService {
    private final VeranstaltungService veranstaltungService;
    private final BenutzerService benutzerService;

    public boolean isEventOwner(Long eventId){
        Benutzer current = benutzerService.getCurrentUserOrNull();
        if(current==null)return false;
        Veranstaltung v = veranstaltungService.getVeranstaltungById(eventId);
        return v != null && v.getBesitzer().equals(current);
    }

    public boolean isSelf(Long userId) {
        var current = benutzerService.getCurrentUserOrNull();
        return current != null && current.getId().equals(userId);
    }
}
