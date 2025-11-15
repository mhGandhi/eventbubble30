package com.lennadi.eventbubble30.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter
public class Veranstaltung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    private Instant creationDate;

    private Instant termin;

    private String title;
    private String description;

    @ManyToOne
    @JoinColumn(name = "besitzer_id")
    private Benutzer besitzer;
}
