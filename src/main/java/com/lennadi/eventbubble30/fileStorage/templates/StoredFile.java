package com.lennadi.eventbubble30.fileStorage.templates;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

//todo auto cleanup
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public  class StoredFile {

    @Getter // TEMPORARY – later reduce visibility //todo
    @Column(name="file_key")
    protected String key;

    protected StoredFile(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("file key must not be null or blank");
        }
        this.key = key;
    }

    public static StoredFile ofStoredKey(String newKey) {
        return new StoredFile(newKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoredFile that)) return false;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
