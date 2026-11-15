package com.apilens.file.repository;

import com.apilens.file.domain.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileRecord, UUID>, JpaSpecificationExecutor<FileRecord> {

    List<FileRecord> findByOrgIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID orgId);

    Optional<FileRecord> findByIdAndOrgIdAndDeletedAtIsNull(UUID id, UUID orgId);

    @Query("""
        SELECT f FROM FileRecord f
        WHERE f.orgId = :orgId AND f.deletedAt IS NULL
          AND LOWER(f.originalName) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY f.createdAt DESC
        """)
    List<FileRecord> autocomplete(
            @Param("orgId") UUID orgId,
            @Param("q") String q);
}
