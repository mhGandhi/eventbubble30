package com.lennadi.eventbubble30.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Benutzer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    private Long id;

    @Getter @Setter
    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
            message = "Benutzername darf nur Buchstaben, Zahlen und _ enthalten")
    private String username;

    @Getter @Setter
    @NotBlank
    private String passwordHash;

    @Getter @Setter
    @Email
    @NotBlank
    private String email;
}
