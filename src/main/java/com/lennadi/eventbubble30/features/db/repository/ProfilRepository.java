package com.lennadi.eventbubble30.features.db.repository;

import com.lennadi.eventbubble30.features.db.entities.Profil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfilRepository extends JpaRepository<Profil, Long> {
    Optional<Profil> getProfilById(long l);

    boolean existsByExternalId(String extId);
    Optional<Profil> getProfilByExternalId(String extId);
    void deleteByExternalId(String extId);
}
