package com.lennadi.eventbubble30.dto;

import com.lennadi.eventbubble30.entities.Benutzer;

public record BenutzerDTO(
        Long id,
        String email,
        String username
) {
    public static BenutzerDTO fromBenutzer(Benutzer benutzer) {
        return new BenutzerDTO(benutzer.getId(), benutzer.getEmail(), benutzer.getUsername());
    }
}