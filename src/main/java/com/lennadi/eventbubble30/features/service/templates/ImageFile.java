package com.lennadi.eventbubble30.features.service.templates;

public abstract class ImageFile extends StoredFile {

    protected ImageFile(String key) {
        super(key);
    }

    public static void validateMimeType(String mimeType) {
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image mime types are allowed");
        }
    }
}
