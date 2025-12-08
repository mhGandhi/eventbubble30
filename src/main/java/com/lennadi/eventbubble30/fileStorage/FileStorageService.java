package com.lennadi.eventbubble30.fileStorage;

import java.io.InputStream;
import java.net.URL;

public interface FileStorageService {
//todo vlt dateien wrappen
    /**
     * Stores the file and returns a unique storage key.
     *
     * @param input the InputStream of the file
     * @param filename original filename for metadata (optional)
     * @param mimeType detected or provided mime type
     * @return a generated key used to reference the stored file
     */
    String store(InputStream input, String filename, String mimeType);

    /**
     * Returns a public or internal URL for serving the file.
     */
    URL getFileURL(String key);

    /**
     * Löscht gespeicherte Datei nach Key. Fehlend eif ignorieren.
     */
    void delete(String key);

    /**
     * Used by MediaController to actually stream file content to users.
     */
    InputStream getFileStream(String key);

    /**
     * Returns the MIME type for a stored file.
     */
    String getMimeType(String key);


    //Todo s3 Speicher (temporäre URLs, andere Domain und anderer Path)
}

