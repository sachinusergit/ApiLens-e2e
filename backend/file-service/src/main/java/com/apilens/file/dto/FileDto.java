package com.apilens.file.dto;

import com.apilens.file.domain.FileRecord;

import java.time.Instant;
import java.util.UUID;

public record FileDto(
        UUID id,
        String originalName,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        String status,
        Instant createdAt) {

    public static FileDto from(FileRecord f) {
        return new FileDto(f.getId(), f.getOriginalName(), f.getContentType(),
                f.getSizeBytes(), f.getChecksumSha256(), f.getStatus(), f.getCreatedAt());
    }
}
