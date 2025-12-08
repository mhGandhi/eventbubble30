package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.features.controller.ProfilController;
import com.lennadi.eventbubble30.features.db.entities.Profil;
import com.lennadi.eventbubble30.features.db.repository.ProfilRepository;
import com.lennadi.eventbubble30.fileStorage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final ProfilRepository profilRepo;
    private final BenutzerService benutzerService;
    private final FileStorageService storage;
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
        if (!file.getContentType().startsWith("image/")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"wrong file type");
        if (file.getSize() > (5 * 1024 * 1024)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"file to large");

        Profil profil = getProfil(profilId);

        // delete previous avatar
        if (profil.getAvatarKey() != null) {
            storage.delete(profil.getAvatarKey());
        }

        // store new file
        String key = storage.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
        );

        profil.setAvatarKey(key);

        return profil;
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional
    public Profil deleteAvatar(long profilId) {
        Profil profil = getProfil(profilId);

        if (profil.getAvatarKey() != null) {
            storage.delete(profil.getAvatarKey());
        }

        profil.setAvatarKey(null);

        return profil;
    }

    public long getCurrentUserId() {
        return benutzerService.getCurrentUser().getId();
    }

    public Profil.DTO toDTO(Profil p){
        return new Profil.DTO(p.getId(), p.getName(), storage.getFileURL(p.getAvatarKey()), p.getBio());
    }

    public Profil.SmallDTO toSmallDTO(Profil p){
        return new Profil.SmallDTO(p.getId(), p.getName(), storage.getFileURL(p.getAvatarKey()));
    }

}
