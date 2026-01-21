package com.lennadi.eventbubble30.security;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.repository.BenutzerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BenutzerDetailsService implements UserDetailsService {

    private final BenutzerRepository repo;

    @Override
    public BenutzerDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var benutzer = repo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return loadUser(benutzer);
    }

    public BenutzerDetails loadUserById(Long userId) {
        var benutzer = repo.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return loadUser(benutzer);
    }

    public BenutzerDetails loadUser(Benutzer benutzer) {
        return new BenutzerDetails(benutzer);
    }
}
