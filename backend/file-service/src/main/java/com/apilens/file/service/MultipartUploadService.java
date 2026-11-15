package com.apilens.file.service;

import com.apilens.file.domain.FileRecord;
import com.apilens.file.dto.FileDto;
import com.apilens.file.repository.FileRepository;
import com.apilens.file.util.FileContentTypes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class MultipartUploadService {

    private static final long MAX_SIZE = 50L * 1024 * 1024;

    private final StorageService storage;
    private final FileRepository repo;

    public MultipartUploadService(StorageService storage, FileRepository repo) {
        this.storage = storage;
        this.repo = repo;
    }

    public FileDto store(UUID orgId, UUID ownerId, MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File exceeds 50 MB — use presigned upload");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        UUID id = UUID.randomUUID();
        String storageKey = orgId + "/" + id + "/" + sanitize(file.getOriginalFilename());
        String contentType =
                FileContentTypes.normalize(file.getOriginalFilename(), file.getContentType());

        String checksum;
        try (InputStream in = file.getInputStream()) {
            checksum = sha256(in);
        } catch (Exception e) {
            throw new RuntimeException("Checksum failed: " + e.getMessage(), e);
        }

        try (InputStream in = file.getInputStream()) {
            storage.put(storageKey, in, file.getSize(), contentType);
        } catch (Exception e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }

        FileRecord record = new FileRecord(
                id, orgId, ownerId,
                file.getOriginalFilename(),
                storageKey,
                contentType,
                file.getSize(),
                checksum,
                "AVAILABLE");

        repo.save(record);
        return FileDto.from(record);
    }

    private static String sha256(InputStream in) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
