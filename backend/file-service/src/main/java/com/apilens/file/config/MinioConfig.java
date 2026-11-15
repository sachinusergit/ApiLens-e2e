package com.apilens.file.config;

import org.springframework.context.annotation.Configuration;

/**
 * MinIO has been discontinued publicly. Storage is now handled by
 * StorageService (local filesystem). This class is intentionally empty
 * to keep the config package structure in place.
 */
@Configuration
public class MinioConfig {
    // Intentionally empty — see StorageService.
}