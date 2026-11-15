package com.apilens.prefs.controller;

import com.apilens.prefs.domain.Confirmation;
import com.apilens.prefs.repository.ConfirmationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/confirmations")
public class ConfirmationsController {

    private final ConfirmationRepository repo;

    public ConfirmationsController(ConfirmationRepository repo) {
        this.repo = repo;
    }

    public record ConfirmBody(
            @NotBlank String action,
            @NotBlank @Pattern(regexp = "YES|NO") String choice,
            String note) {}

    @PostMapping
    public ResponseEntity<?> record(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody ConfirmBody body) {

        Confirmation c = new Confirmation(UUID.randomUUID(), userId, orgId,
                body.action(), body.choice(), body.note());
        repo.save(c);

        String message = "YES".equals(body.choice())
                ? "Confirmed - changes applied"
                : "Cancelled - no changes made";

        return ResponseEntity.ok(Map.of(
                "id", c.getId(),
                "choice", c.getChoice(),
                "message", message,
                "recordedAt", c.getCreatedAt()
        ));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Confirmation>> recent(@RequestHeader("X-Org-Id") UUID orgId) {
        return ResponseEntity.ok(repo.findByOrgIdOrderByCreatedAtDesc(orgId));
    }
}
