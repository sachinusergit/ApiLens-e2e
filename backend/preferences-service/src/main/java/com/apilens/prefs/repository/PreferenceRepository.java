package com.apilens.prefs.repository;

import com.apilens.prefs.domain.Preference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreferenceRepository extends JpaRepository<Preference, UUID> {
    Optional<Preference> findByUserIdAndKey(UUID userId, String key);
    List<Preference> findByUserId(UUID userId);
}
