package com.lennadi.eventbubble30.entities;

import com.fasterxml.jackson.databind.ser.Serializers;
import jakarta.persistence.*;
import lombok.AccessLevel;
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


    public static record DTO(
            Long id,
            Instant creationDate,
            Instant modificationDate,
            Instant termin,
            String title,
            String description,

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
                (this.getBesitzer()!=null?this.getBesitzer().toDTO():null)
        );
    }
}
