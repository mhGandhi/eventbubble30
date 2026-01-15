package com.lennadi.eventbubble30.features.db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Location {

    @Column(name = "location_display_name")
    private String displayName;

    @Column(name = "location_latitude")
    private Double latitude;

    @Column(name = "location_longitude")
    private Double longitude;

    @Column(name = "location_street")
    private String street;

    @Column(name = "location_city")
    private String city;

    @Column(name = "location_postal_code")
    private String postalCode;

    @Column(name = "location_country")
    private String country;

    @Column(name = "location_external_source")
    private String externalSource;

    @Column(name = "location_external_id")
    private String externalId;


    @Override
    public String toString() {
        return "Location{" +
                "displayName='" + displayName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", externalSource='" + externalSource + '\'' +
                ", externalId='" + externalId + '\'' +
                '}';
    }
}
