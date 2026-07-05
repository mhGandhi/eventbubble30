package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.fileStorage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storage;

    @GetMapping("/{key}")
    public ResponseEntity<?> getFile(@PathVariable String key) {

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
