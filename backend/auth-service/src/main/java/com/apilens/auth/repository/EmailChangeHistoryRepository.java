package com.apilens.auth.repository;

import com.apilens.auth.domain.EmailChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EmailChangeHistoryRepository extends JpaRepository<EmailChangeHistory, UUID> {

    long countByUserIdAndChangedAtAfter(UUID userId, Instant since);

    List<EmailChangeHistory> findTop20ByUserIdOrderByChangedAtDesc(UUID userId);
}
