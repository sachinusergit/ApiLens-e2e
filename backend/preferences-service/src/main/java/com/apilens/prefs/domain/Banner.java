package com.apilens.prefs.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "banners")
public class Banner {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cta_url", length = 500)
    private String ctaUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Banner() {}

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getImageUrl() { return imageUrl; }
    public String getCtaUrl() { return ctaUrl; }
    public boolean isActive() { return active; }
    public Integer getPriority() { return priority; }
    public Instant getCreatedAt() { return createdAt; }
}
