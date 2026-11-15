package com.apilens.file.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem storage. Replaces MinIO (which is no longer publicly available).
 * Files are stored under STORAGE_ROOT/<key>.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path root;

    public StorageService(@Value("${app.storage.root:/app/data/files}") String rootDir) {
        this.root = Paths.get(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("Storage ready at: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage dir: " + root, e);
        }
    }

    public void put(String key, InputStream data, long size, String contentType) {
        try {
            Path target = resolveSafe(key);
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} ({} bytes)", key, size);
        } catch (IOException e) {
            throw new RuntimeException("File store failed: " + e.getMessage(), e);
        }
    }

    public InputStream get(String key) {
        try {
            Path target = resolveSafe(key);
            if (!Files.exists(target)) {
                throw new RuntimeException("File not found on disk: " + key);
            }
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new RuntimeException("File read failed: " + e.getMessage(), e);
        }
    }

    public void delete(String key) {
        try {
            Path target = resolveSafe(key);
            Files.deleteIfExists(target);
            log.info("Deleted file: {}", key);
        } catch (IOException e) {
            throw new RuntimeException("File delete failed: " + e.getMessage(), e);
        }
    }

    private Path resolveSafe(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return p;
    }
}