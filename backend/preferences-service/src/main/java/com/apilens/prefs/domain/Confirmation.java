package com.apilens.prefs.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "confirmations")
public class Confirmation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 10)
    private String choice;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Confirmation() {}

    public Confirmation(UUID id, UUID userId, UUID orgId, String action, String choice, String note) {
        this.id = id;
        this.userId = userId;
        this.orgId = orgId;
        this.action = action;
        this.choice = choice;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getOrgId() { return orgId; }
    public String getAction() { return action; }
    public String getChoice() { return choice; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
