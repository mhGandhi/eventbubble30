package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.fileStorage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storage;

    @GetMapping("/{key}")
    public ResponseEntity<?> getFile(@PathVariable String key) {

        InputStream stream = storage.getFileStream(key);
        if (stream == null) {
            return ResponseEntity.notFound().build();
        }

        String mime = storage.getMimeType(key);
        MediaType contentType = (mime != null)
                ? MediaType.parseMediaType(mime)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity
                .ok()
                .contentType(contentType)
                .body(new InputStreamResource(stream));
    }

}
