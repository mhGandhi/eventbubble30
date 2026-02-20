package com.lennadi.eventbubble30.features.db.entities;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.Location;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "veranstaltung",
        indexes = {
                @Index(name = "idx_event_termin", columnList = "termin"),
                @Index(name = "idx_event_created", columnList = "creation_date"),
                @Index(name = "idx_event_modified", columnList = "modification_date"),
                @Index(name = "idx_event_owner", columnList = "besitzer_id"),
                @Index(name = "idx_event_city", columnList = "location_city"),
                @Index(name = "idx_event_lat_lon", columnList = "location_latitude,location_longitude"),
                @Index(name = "idx_event_title", columnList = "title"),
                @Index(name = "idx_event_external_id", columnList = "external_id")
        }
)
@Getter @Setter
public class Veranstaltung extends BaseEntity {
    public static final EntityType TYPE = EntityType.EVENT;

    private Instant termin;

    private String title;
    private String description;

    @ManyToOne
    @JoinColumn(name = "besitzer_id")
    private Benutzer besitzer;

    @Embedded
    private Location location;

    @Override
    public EntityType getType() {
        return TYPE;
    }


    public static record DTO(
            String id,
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
                this.getExternalId(),
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
