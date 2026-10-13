package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.fileStorage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storage;

    private static final String SHARED_STORAGE_KEYHAHAHA = "abcdiekatzeliefimschnee";

    @GetMapping("/{key}")
    public ResponseEntity<?> getFile(
            @PathVariable String key,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {

        // 1. Password protection check
        if (isProtected(key) && !isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Restricted Files\"")
                    .body("Authentication required");
        }



        /*
        InputStream stream = storage.getFileStream(key);
        if (stream == null) {
            return ResponseEntity.notFound().build();
        }

        String mime = storage.getMimeType(key);
        MediaType contentType = (mime != null)
                ? MediaType.parseMediaType(mime)
                : MediaType.APPLICATION_OCTET_STREAM;

        String extension = extensionFromMimeType(mime);
        String filename = (extension == null || extension.isBlank()) ? key : key + "." + extension;

        return ResponseEntity
                .ok()
                .contentType(contentType)
                .body(new InputStreamResource(stream));*/

        Resource resource = storage.getFileResource(key);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        String mime = storage.getMimeType(key);
        MediaType contentType = (mime != null)
                ? MediaType.parseMediaType(mime)
                : MediaType.APPLICATION_OCTET_STREAM;

        String extension = extensionFromMimeType(mime);
        String filename = (extension == null || extension.isBlank()) ? key : key + "." + extension;

        try{
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(resource.contentLength())
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline()
                                    .filename(filename, StandardCharsets.UTF_8)
                                    .build()
                                    .toString()
                    )
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }catch (Exception ex){
            return ResponseEntity.internalServerError().build();
        }
    }


    private boolean isProtected(String key) {
        // Replace with your condition, list of IDs, or database flag
        Set<String> keys = Set.of("minecraft", "daslebenderanderen", "fnaf2_fuer_ece_viel_spass");
        return keys.contains(key);
    }

    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }
        try {
            String base64 = authHeader.substring(6).trim();
            String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);

            // parts[1] is the password in a "user:pass" payload
            String providedPassword = parts.length > 1 ? parts[1] : parts[0];
            return SHARED_STORAGE_KEYHAHAHA.equals(providedPassword);
        } catch (Exception e) {
            return false;
        }
    }




    private String extensionFromMimeType(String mimeType) {
        if (mimeType == null) return null;

        return switch (mimeType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            case "video/ogg" -> "ogv";
            case "audio/mpeg" -> "mp3";
            case "audio/ogg" -> "ogg";
            case "application/pdf" -> "pdf";
            default -> null;
        };
    }

}
