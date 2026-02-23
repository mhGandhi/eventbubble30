package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.controller.ProfilController;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.Profil;
import com.lennadi.eventbubble30.features.db.repository.ProfilRepository;
import com.lennadi.eventbubble30.fileStorage.FileManagerService;
import com.lennadi.eventbubble30.fileStorage.templates.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final ProfilRepository profilRepo;
    private final BenutzerService benutzerService;
    private final FileManagerService fileManagerService;
    /// //////////////////////////////////////////////////////////////

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public Profil createProfil(String extId, ProfilController.CreateProfilRequest request) {
        if(profilRepo.existsByExternalId(extId)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profil besteht bereits");
        }

        Benutzer benutzer = benutzerService.getBenutzer(extId);//todo tmp

        Profil neu = new Profil(benutzer.getId(), benutzer.getExternalId(), request.name());
        neu.setName(request.name());
        neu.setBio(request.bio());

        return profilRepo.save(neu);
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public Profil updateProfil(String extId, ProfilController.UpdateProfilRequest updateProfilRequest) {
        Profil profil = getProfil(extId);
        if(updateProfilRequest.name()!=null)
            profil.setName(updateProfilRequest.name());

        if(updateProfilRequest.bio()!=null)
            profil.setBio(updateProfilRequest.bio());

        return profil;
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public void deleteProfil(String extId){
        profilRepo.deleteByExternalId(extId);
    }

    @Transactional(readOnly = true)
    public Profil getProfil(String extId) {
        return profilRepo.getProfilByExternalId(extId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"/api/profil/"+extId));
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public Profil updateAvatar(String profilExtId, MultipartFile file) throws IOException {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wrong file type");
        }

        if (file.getSize() > (5 * 1024 * 1024)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"file to large");

        Profil profil = getProfil(profilExtId);

        StoredFile oldAvatar = profil.getAvatar();

        profil.setAvatar(fileManagerService.toProfilePicture(file));

        // delete previous avatar
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (oldAvatar != null) fileManagerService.delete(oldAvatar);
                    }
                }
        );

        return profil;
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public void deleteAvatar(String profilExtId) {
        Profil profil = getProfil(profilExtId);
        if (profil.hasAvatar()) {
            fileManagerService.delete(profil.getAvatar());
        }
        profil.setAvatar(null);
    }

    public URL getAvatarUrl(String profilExtId) {
        Profil profil = getProfil(profilExtId);
        if(profil==null || !profil.hasAvatar()) return null;
        return fileManagerService.getURL(profil.getAvatar());
    }

    public long getCurrentUserId() {
        return benutzerService.getCurrentUser().getId();
    }

    public String getCurrentUserExternalId(){
        return benutzerService.getCurrentUser().getExternalId();
    }

    public boolean exists(String extId) {
        return profilRepo.existsByExternalId(extId);
    }
}
