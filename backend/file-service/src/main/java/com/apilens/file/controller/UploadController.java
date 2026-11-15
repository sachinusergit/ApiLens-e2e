package com.apilens.file.controller;

import com.apilens.file.dto.FileDto;
import com.apilens.file.service.MultipartUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/files")
public class UploadController {

    private final MultipartUploadService service;

    public UploadController(MultipartUploadService service) {
        this.service = service;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> upload(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        UUID orgId = orgIdHeader != null ? UUID.fromString(orgIdHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = userIdHeader != null ? UUID.fromString(userIdHeader) : UUID.fromString("11111111-1111-1111-1111-111111111111");

        FileDto dto = service.store(orgId, userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
