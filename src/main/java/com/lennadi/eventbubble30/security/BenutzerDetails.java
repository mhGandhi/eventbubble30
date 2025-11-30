package com.lennadi.eventbubble30.security;

import com.lennadi.eventbubble30.features.entities.Benutzer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

public class BenutzerDetails implements UserDetails {

    @Getter
    private final Long id;
    private final String username;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;


    @Getter
    private final Instant passwordChangedAt;
    @Getter
    private final Instant tokensInvalidatedAt;

    public BenutzerDetails(Benutzer b) {
        this.id = b.getId();
        this.username = b.getUsername();
        this.passwordHash = b.getPasswordHash();
        // Convert roles → Spring Security authorities
        this.authorities = b.getRoles().stream()
                .map(Benutzer.Role::name)                     // USER → "USER"
                .map(r -> "ROLE_" + r)               // add prefix
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());

        this.passwordChangedAt = b.getPasswordChangedAt();
        this.tokensInvalidatedAt = b.getTokensInvalidatedAt();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Optional später erweitern
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Optional später erweitern
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Optional später erweitern
    }

    @Override
    public boolean isEnabled() {
        return true; // Optional später erweitern
    }
}
