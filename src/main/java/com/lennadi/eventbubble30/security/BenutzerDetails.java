package com.lennadi.eventbubble30.security;

import com.lennadi.eventbubble30.entities.Benutzer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class BenutzerDetails implements UserDetails {

    @Getter
    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String role;

    public BenutzerDetails(Benutzer b) {
        this.id = b.getId();
        this.username = b.getUsername();
        this.passwordHash = b.getPasswordHash();
        this.role = b.getRole();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
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
