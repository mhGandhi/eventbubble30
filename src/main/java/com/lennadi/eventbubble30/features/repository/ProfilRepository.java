package com.lennadi.eventbubble30.features.repository;

import com.lennadi.eventbubble30.features.entities.Profil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfilRepository extends JpaRepository<Profil, Long> {

    Optional<Profil> findByBenutzer_Id(Long benutzerId);
}
