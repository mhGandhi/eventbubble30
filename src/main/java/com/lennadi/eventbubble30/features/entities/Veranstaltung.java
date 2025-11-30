package com.lennadi.eventbubble30.features.entities;

import com.lennadi.eventbubble30.features.Location;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter
public class Veranstaltung extends BaseEntity {
    private Instant termin;

    private String title;
    private String description;

    @ManyToOne
    @JoinColumn(name = "besitzer_id")
    private Benutzer besitzer;

    @Embedded
    private Location location;


    public static record DTO(
            Long id,
            Instant creationDate,
            Instant modificationDate,
            Instant termin,
            String title,
            String description,
            Location location,

            Benutzer.DTO besitzer
    ) {}

    public DTO toDTO() {
        return new DTO(
                this.getId(),
                this.getCreationDate(),
                this.getModificationDate(),
                this.getTermin(),
                this.getTitle(),
                this.getDescription(),
                this.getLocation(),
                (this.getBesitzer()!=null?this.getBesitzer().toDTO():null)
        );
    }
}
