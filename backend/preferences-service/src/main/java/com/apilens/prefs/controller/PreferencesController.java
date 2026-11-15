package com.apilens.prefs.controller;

import com.apilens.prefs.domain.Preference;
import com.apilens.prefs.repository.PreferenceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/preferences")
public class PreferencesController {

    private final PreferenceRepository repo;

    public PreferencesController(PreferenceRepository repo) {
        this.repo = repo;
    }

    public record SliderBody(
            @NotBlank String key,
            @NotNull @Min(0) @Max(100) Integer value) {}

    @PostMapping("/slider")
    public ResponseEntity<?> setSlider(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody SliderBody body) {

        Preference p = repo.findByUserIdAndKey(userId, body.key())
                .orElseGet(() -> new Preference(UUID.randomUUID(), userId, orgId, body.key(), body.value()));

        p.setValue(body.value());
        repo.save(p);

        return ResponseEntity.ok(Map.of(
                "id", p.getId(),
                "key", p.getKey(),
                "value", p.getValue(),
                "savedAt", p.getUpdatedAt()
        ));
    }

    @GetMapping("/slider")
    public ResponseEntity<List<Preference>> getAll(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(repo.findByUserId(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "preferences-service"));
    }
}
