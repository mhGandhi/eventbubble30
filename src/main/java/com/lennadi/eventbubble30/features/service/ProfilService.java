package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.controller.ProfilController;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.Profil;
import com.lennadi.eventbubble30.features.db.repository.ProfilRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final ProfilRepository profilRepository;
    private final BenutzerService benutzerService;
    /// //////////////////////////////////////////////////////////////

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public Profil createProfil(long id , ProfilController.CreateProfilRequest request) {
        Profil neu = new Profil(id, request.name());
        neu.setName(request.name());
        neu.setBio(request.bio());

        return profilRepository.save(neu);
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public Profil updateProfil(long id, ProfilController.UpdateProfilRequest updateProfilRequest) {
        Profil profil = getProfil(id);
        if(updateProfilRequest.name()!=null)
            profil.setName(updateProfilRequest.name());

        if(updateProfilRequest.bio()!=null)
            profil.setBio(updateProfilRequest.bio());

        return profil;
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public void deleteProfil(long id){
        profilRepository.deleteById(id);
    }

    public Profil getProfil(long id) {
        return profilRepository.getProfilById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"/api/profil/"+id));
    }

    public long getCurrentUserId() {
        return benutzerService.getCurrentUser().getId();
    }

}
