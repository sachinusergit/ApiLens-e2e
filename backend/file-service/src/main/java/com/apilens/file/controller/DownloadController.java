package com.apilens.file.controller;

import com.apilens.file.domain.FileRecord;
import com.apilens.file.dto.FileDto;
import com.apilens.file.repository.FileRepository;
import com.apilens.file.service.StorageService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class DownloadController {

    private final FileRepository repo;
    private final StorageService storage;

    public DownloadController(FileRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @GetMapping
    public ResponseEntity<List<FileDto>> list(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        UUID orgId = orgIdHeader != null ? UUID.fromString(orgIdHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<FileDto> list = repo.findByOrgIdAndDeletedAtIsNullOrderByCreatedAtDesc(orgId)
                .stream().map(FileDto::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @PathVariable UUID id) {

        UUID orgId = orgIdHeader != null ? UUID.fromString(orgIdHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        FileRecord rec = repo.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (!"AVAILABLE".equals(rec.getStatus())) {
            return ResponseEntity.status(403).build();
        }

        InputStream stream = storage.get(rec.getStorageKey());
        StreamingResponseBody body = out -> {
            try (InputStream in = stream) {
                in.transferTo(out);
            }
        };

        MediaType mediaType = rec.getContentType() != null
                ? MediaType.parseMediaType(rec.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(rec.getOriginalName()).build().toString())
                .contentType(mediaType)
                .contentLength(rec.getSizeBytes())
                .header("X-Checksum-SHA256", rec.getChecksumSha256() != null ? rec.getChecksumSha256() : "")
                .body(body);
    }

    @GetMapping("/{id}/meta")
    public ResponseEntity<FileDto> meta(@PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        UUID orgId = orgIdHeader != null ? UUID.fromString(orgIdHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        FileRecord rec = repo.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        return ResponseEntity.ok(FileDto.from(rec));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @PathVariable UUID id) {

        UUID orgId = orgIdHeader != null ? UUID.fromString(orgIdHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        FileRecord rec = repo.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        rec.setStatus("DELETED");
        rec.setDeletedAt(java.time.Instant.now());
        repo.save(rec);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "file-service"));
    }
}
