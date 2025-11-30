package com.lennadi.eventbubble30.features.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Profil extends BaseEntity {
    @OneToOne
    @JoinColumn(nullable = false, updatable = false, unique = true, name="benutzer_id")
    private Benutzer benutzer;

    private String name;
    private String bio;

    public DTO toDTO(){
        return new DTO(getBenutzer().toDTO(), getName(), getBio());
    }
    public record DTO(Benutzer.DTO benutzer, String name, String bio) {}
}
