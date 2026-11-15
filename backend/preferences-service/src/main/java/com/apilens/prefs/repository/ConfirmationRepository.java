package com.apilens.prefs.repository;

import com.apilens.prefs.domain.Confirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConfirmationRepository extends JpaRepository<Confirmation, UUID> {
    List<Confirmation> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
}
