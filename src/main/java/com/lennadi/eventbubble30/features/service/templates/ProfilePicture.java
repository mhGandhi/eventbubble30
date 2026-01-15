package com.lennadi.eventbubble30.features.service.templates;

public class ProfilePicture extends ImageFile {

    private ProfilePicture(String key) {
        super(key);
    }

    /**
     * Factory method used by services AFTER validation & storage
     */
    public static ProfilePicture ofStoredKey(String key) {
        return new ProfilePicture(key);
    }

    /**
     * Validation entry point used BEFORE storing
     */
    public static void validateUpload(String mimeType, long sizeBytes) {
        validateMimeType(mimeType);

        if (sizeBytes > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Profile picture too large (max 5MB)");
        }
    }
}
