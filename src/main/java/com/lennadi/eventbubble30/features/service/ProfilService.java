package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.controller.ProfilController;
import com.lennadi.eventbubble30.features.db.entities.Profil;
import com.lennadi.eventbubble30.features.db.repository.ProfilRepository;
import com.lennadi.eventbubble30.features.service.templates.ProfilePicture;
import com.lennadi.eventbubble30.features.service.templates.StoredFile;
import com.lennadi.eventbubble30.fileStorage.FileManagerService;
import com.lennadi.eventbubble30.fileStorage.FileStorageService;
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
    public Profil createProfil(long id , ProfilController.CreateProfilRequest request) {
        if(profilRepo.existsById(id)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profil besteht bereits");
        }

        Profil neu = new Profil(id, request.name());
        neu.setName(request.name());
        neu.setBio(request.bio());

        return profilRepo.save(neu);
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
    @Transactional
    public void deleteProfil(long id){
        profilRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Profil getProfil(long id) {
        return profilRepo.getProfilById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"/api/profil/"+id));
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public Profil updateAvatar(long profilId, MultipartFile file) throws IOException {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "wrong file type");
        }

        if (file.getSize() > (5 * 1024 * 1024)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"file to large");

        Profil profil = getProfil(profilId);

        ProfilePicture oldAvatar = profil.getAvatar();

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
    public void deleteAvatar(long profilId) {
        Profil profil = getProfil(profilId);
        ProfilePicture oldAvatar = profil.getAvatar();
        if (oldAvatar != null) {
            fileManagerService.delete(oldAvatar);
        }

        profil.setAvatar(null);
    }

    public URL getAvatarUrl(long profilId) {
        Profil profil = getProfil(profilId);
        if(profil==null || profil.getAvatar()==null) return null;
        return fileManagerService.getURL(profil.getAvatar());
    }

    public long getCurrentUserId() {
        return benutzerService.getCurrentUser().getId();
    }

    public Profil.DTO toDTO(Profil p){
        return new Profil.DTO(p.getId(), p.getName(), fileManagerService.getURL(p.getAvatar()), p.getBio());
    }

    public Profil.SmallDTO toSmallDTO(Profil p){
        return new Profil.SmallDTO(p.getId(), p.getName(), fileManagerService.getURL(p.getAvatar()));
    }

    public boolean exists(long id) {
        return profilRepo.existsById(id);
    }
}
