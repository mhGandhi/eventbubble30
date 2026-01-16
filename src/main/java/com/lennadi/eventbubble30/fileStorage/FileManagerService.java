package com.lennadi.eventbubble30.fileStorage;

import com.lennadi.eventbubble30.fileStorage.templates.StoredFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileManagerService {
    private final FileStorageService fileStorageService;

    public StoredFile toProfilePicture(MultipartFile file) throws IOException {

        String newKey = fileStorageService.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
        );

        StoredFile ret = null;
        try{
            ret = StoredFile.ofStoredKey(newKey);
        }catch(Exception e){
            fileStorageService.delete(newKey);
        }

        return ret;
    }

    public URL getURL(StoredFile storedFile, int ttlSec) {
        Instant then =  Instant.now().plusSeconds(ttlSec);
        return getURL(storedFile, then);
    }

    public URL getURL(StoredFile storedFile, Instant expiry) {
        if(storedFile == null){
            log.warn("stored file is null");
            return null;
        }
        try{
            return fileStorageService.getFileURL(storedFile.getKey());//todo expiry
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public URL getURL(StoredFile storedFile) {
        return getURL(storedFile, null);
    }

    public void delete(StoredFile file) {
        if(file == null){
            log.warn("trying to delete null-file");
            return;
        }
        fileStorageService.delete(file.getKey());
    }
}
