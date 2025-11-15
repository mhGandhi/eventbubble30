package com.lennadi.eventbubble30.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}
