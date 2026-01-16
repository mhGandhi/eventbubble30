package com.lennadi.eventbubble30.fileStorage;

import com.lennadi.eventbubble30.features.service.templates.ProfilePicture;
import com.lennadi.eventbubble30.features.service.templates.StoredFile;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FileManagerService {
    private final FileStorageService fileStorageService;

    public ProfilePicture toProfilePicture(MultipartFile file) throws IOException {
        ProfilePicture.validateUpload(file.getContentType(), file.getSize());

        String newKey = fileStorageService.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
        );

        ProfilePicture ret = null;
        try{
            ret = ProfilePicture.ofStoredKey(newKey);
        }catch(Exception e){
            fileStorageService.delete(newKey);
        }

        return ret;
    }

    public URL getURL(@NotNull StoredFile storedFile, int ttlSec) {
        Instant then =  Instant.now().plusSeconds(ttlSec);
        return getURL(storedFile, then);
    }

    public URL getURL(@NotNull StoredFile storedFile, Instant expiry) {
        try{
            return fileStorageService.getFileURL(storedFile.getKey());//todo expiry
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public URL getURL(@NotNull StoredFile storedFile) {
        return getURL(storedFile, null);
    }

    public void delete(@NotNull StoredFile file) {
        fileStorageService.delete(file.getKey());
    }
}
