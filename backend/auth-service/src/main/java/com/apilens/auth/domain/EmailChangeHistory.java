package com.apilens.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_change_history")
public class EmailChangeHistory {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "old_email", nullable = false, length = 320)
    private String oldEmail;

    @Column(name = "new_email", nullable = false, length = 320)
    private String newEmail;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    public EmailChangeHistory() {}

    public EmailChangeHistory(UUID id, UUID userId, String oldEmail, String newEmail,
                              Instant changedAt, String ipAddress, String userAgent) {
        this.id = id;
        this.userId = userId;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
        this.changedAt = changedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getOldEmail() { return oldEmail; }
    public String getNewEmail() { return newEmail; }
    public Instant getChangedAt() { return changedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
}
