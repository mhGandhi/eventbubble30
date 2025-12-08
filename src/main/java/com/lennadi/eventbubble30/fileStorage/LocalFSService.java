package com.lennadi.eventbubble30.fileStorage;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Profile("localStorage")
@RequiredArgsConstructor
public class LocalFSService implements FileStorageService {

    @Value("${storage.local.base-path:/app/uploads}")
    private String basePath;

    @Value("${storage.local.base-url:http://localhost:8080/media/}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private Path resolveFilePath(String key) {
        return Path.of(basePath, key);
    }

    private Path resolveMetaPath(String key) {
        return Path.of(basePath, key + ".meta");
    }

    private record LocalMeta(String mimeType, long size) {}

    // -------------------------------------------------------------------------
    // Store File
    // -------------------------------------------------------------------------

    @Override
    public String store(InputStream input, String filename, String mimeType) {
        String key = UUID.randomUUID().toString();

        Path filePath = resolveFilePath(key);
        Path metaPath = resolveMetaPath(key);

        try {
            // Make sure base directory exists
            Files.createDirectories(filePath.getParent());

            // Write binary file
            Files.copy(input, filePath);

            // Determine size after writing
            long size = Files.size(filePath);

            // Write metadata JSON
            LocalMeta meta = new LocalMeta(mimeType, size);
            Files.writeString(metaPath, objectMapper.writeValueAsString(meta));

            return key;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file in LocalFSService", e);
        }
    }

    // -------------------------------------------------------------------------
    // Get File URL (for DTOs)
    // -------------------------------------------------------------------------

    @Override
    public URL getFileURL(String key) {
        try {
            return new URL(baseUrl + key);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid base URL for LocalFSService", e);
        }
    }

    // -------------------------------------------------------------------------
    // File Read
    // -------------------------------------------------------------------------

    @Override
    public InputStream getFileStream(String key) {
        Path filePath = resolveFilePath(key);
        try {
            return Files.exists(filePath) ? Files.newInputStream(filePath) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getMimeType(String key) {
        Path metaPath = resolveMetaPath(key);
        if (!Files.exists(metaPath)) return null;

        try {
            String json = Files.readString(metaPath);
            LocalMeta meta = objectMapper.readValue(json, LocalMeta.class);
            return meta.mimeType();
        } catch (IOException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Delete File + Metadata
    // -------------------------------------------------------------------------

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveFilePath(key));
            Files.deleteIfExists(resolveMetaPath(key));
        } catch (IOException ignored) {}
    }
}
