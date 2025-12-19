package com.lennadi.eventbubble30.fileStorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("memoryStorage")
public class MemoryFSService implements FileStorageService{

    private static class MemoryFile {
        byte[] data;
        String mimeType;

        MemoryFile(byte[] data, String mimeType) {
            this.data = data;
            this.mimeType = mimeType;
        }
    }

    @Value("${storage.memory.base-url:http://localhost:8080/media/}")
    private String baseUrl;

    private final Map<String, MemoryFile> files = new ConcurrentHashMap<>();

    @Override
    public String store(InputStream input, String filename, String mimeType) {
        String key = UUID.randomUUID().toString();

        try {
            files.put(key, new MemoryFile(input.readAllBytes(),  mimeType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return key;
    }

    @Override
    public URL getFileURL(String key) {
        if(key==null)return null;
        if(!files.containsKey(key)) return null;
        try {
            return URI.create(baseUrl).resolve(key).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String key) {
        files.remove(key);
    }

    @Override
    public InputStream getFileStream(String key) {
        MemoryFile file = files.get(key);
        if (file == null || file.data == null) return null;
        return new ByteArrayInputStream(file.data);
    }

    @Override
    public String getMimeType(String key) {
        MemoryFile file = files.get(key);
        if (file == null) return null;
        return file.mimeType;
    }

}
