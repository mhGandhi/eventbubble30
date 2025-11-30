package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.repository.ProfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final ProfilRepository profilRepository;
    private final BenutzerService benutzerService;

    //todo
}
