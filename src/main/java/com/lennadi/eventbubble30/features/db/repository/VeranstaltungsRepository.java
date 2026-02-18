package com.lennadi.eventbubble30.features.db.repository;

import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface VeranstaltungsRepository extends
        JpaRepository<Veranstaltung, Long>,
        JpaSpecificationExecutor<Veranstaltung> {

    Optional<Veranstaltung> findByExternalIdIgnoreCase(String externalId);

    public enum OrderBy {
        creationDate,
        modificationDate,
        termin
    }

    public enum OrderDir {
        asc, desc
    }

    public class Specs {

        public static Specification<Veranstaltung> textSearch(String q) {
            return (root, query, cb) -> {
                if (q == null || q.isBlank()) return null;

                String like = "%" + q.toLowerCase() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                );
            };
        }

        public static Specification<Veranstaltung> inCity(String city) {//todo zu instabil (Köln, koeln, köln, cologne etc) -> stattdessen plz oder so?
            return (root, query, cb) -> {
                if (city == null) return null;
                return cb.equal(
                        cb.lower(root.get("location").get("city")),
                        city.toLowerCase()
                );
            };
        }

        public static Specification<Veranstaltung> inBoundingBox(
                Double minLat, Double minLon,
                Double maxLat, Double maxLon
        ) {
            return (root, query, cb) -> {
                if (minLat == null) return null;

                Path<Double> lat = root.get("location").get("latitude");
                Path<Double> lon = root.get("location").get("longitude");

                return cb.and(
                        cb.between(lat, minLat, maxLat),
                        cb.between(lon, minLon, maxLon)
                );
            };
        }

        public static Specification<Veranstaltung> near(//todo cap
                                                        Double lat, Double lon, Double radiusKm
        ) {
            return (root, query, cb) -> {
                if (lat == null || lon == null || radiusKm == null) return null;


                // Rough conversion (good enough up to ~100km)
                double latDelta = radiusKm / 111.0;
                double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

                double minLat = lat - latDelta;
                double maxLat = lat + latDelta;
                double minLon = lon - lonDelta;
                double maxLon = lon + lonDelta;

                Path<Double> latExpr = root.get("location").get("latitude");
                Path<Double> lonExpr = root.get("location").get("longitude");

                return cb.and(
                        cb.between(latExpr, minLat, maxLat),
                        cb.between(lonExpr, minLon, maxLon)
                );
            };
        }

        public static Specification<Veranstaltung> ownedBy(String userExtId) {
            return (root, query, cb) -> {
                if (userExtId == null) return null;
                return cb.equal(root.get("besitzer").get("external_id"), userExtId);
            };
        }


        public static Specification<Veranstaltung> dateBetween(
                Instant from, Instant to
        ) {
            return (root, query, cb) -> {
                if (from == null && to == null) return null;

                if (from != null && to != null)
                    return cb.between(root.get("termin"), from, to);

                if (from != null)
                    return cb.greaterThanOrEqualTo(root.get("termin"), from);

                return cb.lessThanOrEqualTo(root.get("termin"), to);
            };
        }
    }
}
