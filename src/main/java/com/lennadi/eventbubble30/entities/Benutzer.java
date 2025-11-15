package com.lennadi.eventbubble30.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Entity
@Getter
@Setter
public class Benutzer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @OneToMany(mappedBy = "besitzer")
    private Collection<Veranstaltung> veranstaltungen;


    public BenutzerDTO toDTO() {
        return new BenutzerDTO(this.getId(), this.getEmail(), this.getUsername());
    }

    public static record BenutzerDTO(
            Long id,
            String email,
            String username
    ) { }
}
