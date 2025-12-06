package com.lennadi.eventbubble30.features.db;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter @Setter
public class Location {//todo entity vlt
    private String displayName;

    private Double latitude;
    private Double longitude;

    private String street;
    private String city;
    private String postalCode;
    private String country;

    private String externalSource;//z.B. Nominatim
    private String externalId;
}
