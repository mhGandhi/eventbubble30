package com.lennadi.eventbubble30.security;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
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

    private final boolean emailVerified;


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
        this.emailVerified = b.isEmailVerified();
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
        if (authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }
        return emailVerified;
        // Optional später erweitern
    }
}
