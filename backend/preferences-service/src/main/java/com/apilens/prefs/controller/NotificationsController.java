package com.apilens.prefs.controller;

import com.apilens.prefs.domain.Notification;
import com.apilens.prefs.repository.NotificationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    private final NotificationRepository repo;

    public NotificationsController(NotificationRepository repo) {
        this.repo = repo;
    }

    public record NotifyBody(
            @NotBlank @Pattern(regexp = "success|error|info|warning") String type,
            @NotBlank String message) {}

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody NotifyBody body) {

        Notification n = new Notification(UUID.randomUUID(), userId, orgId, body.type(), body.message());
        repo.save(n);

        return ResponseEntity.status(201).body(Map.of(
                "id", n.getId(),
                "type", n.getType(),
                "message", n.getMessage(),
                "createdAt", n.getCreatedAt()
        ));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> list(@RequestHeader("X-Org-Id") UUID orgId) {
        return ResponseEntity.ok(repo.findByOrgIdOrderByCreatedAtDesc(orgId));
    }
}
