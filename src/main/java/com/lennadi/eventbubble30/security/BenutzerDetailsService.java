package com.lennadi.eventbubble30.security;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BenutzerDetailsService implements UserDetailsService {

    private final BenutzerRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var benutzer = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return loadUser(benutzer);
    }

    public Object loadUserById(Long userId) {
        var benutzer = repo.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return loadUser(benutzer);
    }

    public UserDetails loadUser(Benutzer benutzer) {
        return new BenutzerDetails(benutzer);
    }
}
