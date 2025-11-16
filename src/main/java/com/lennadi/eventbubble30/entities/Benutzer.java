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
    //todo erweitertes Profil (separate Tabelle? - Bio, Name, Avatar etc.)

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false, length = 255)
    private String email;
    //todo email verification (+removal job nach nh woche ohne)

    @OneToMany(mappedBy = "besitzer")
    private Collection<Veranstaltung> veranstaltungen;


    public DTO toDTO() {
        return new DTO(this.getId(), this.getUsername());
    }

    public static record DTO(
            Long id,
            String username
    ) { }
}
