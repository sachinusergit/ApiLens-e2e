package com.apilens.file.dto;

import com.apilens.file.domain.FileRecord;
import com.apilens.file.util.FileContentTypes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Search envelope: meta + summary + files.
 * <p>
 * Example:
 * {
 *   "meta":    { "totalCount": 1, "filterApplied": "contentType=text/plain",
 *                "executionTimeMs": 8, "orgId": "..." },
 *   "summary": { "totalFiles": 1, "totalSizeHuman": "1.0 KB",
 *                "byCategory": { "document": 1 } },
 *   "files":   [ ... ]
 * }
 */
public record SearchResponse(Meta meta, Summary summary, List<FileView> files) {

    public record Meta(int totalCount, String filterApplied, long executionTimeMs, String orgId) {
    }

    public record Summary(int totalFiles, String totalSizeHuman, Map<String, Integer> byCategory) {
    }

    public record FileView(
            String id,
            String originalName,
            String contentType,
            long sizeBytes,
            String sizeHuman,
            String category,
            String extension,
            boolean canPreview,
            String status,
            String createdAtHuman,
            List<String> tags) {
    }

    public static SearchResponse of(
            List<FileRecord> records,
            String filterApplied,
            UUID orgId,
            long executionTimeMs) {

        List<FileView> views = new ArrayList<>();
        Map<String, Integer> byCategory = new TreeMap<>();
        long totalSize = 0L;

        for (FileRecord record : records) {
            String contentType =
                    FileContentTypes.normalize(record.getOriginalName(), record.getContentType());
            String category = FileContentTypes.category(contentType);
            String extension = FileContentTypes.extension(record.getOriginalName());
            long size = record.getSizeBytes();

            totalSize += size;
            byCategory.merge(category, 1, Integer::sum);

            LinkedHashSet<String> tags = new LinkedHashSet<>();
            tags.add(category);
            if (!extension.isEmpty()) {
                tags.add(extension.substring(1));
            }
            if (FileContentTypes.isToday(record.getCreatedAt())) {
                tags.add("uploaded-today");
            }
            tags.add(statusTag(record.getStatus()));

            views.add(new FileView(
                    record.getId().toString(),
                    record.getOriginalName(),
                    contentType,
                    size,
                    FileContentTypes.sizeHuman(size),
                    category,
                    extension,
                    FileContentTypes.canPreview(contentType),
                    record.getStatus(),
                    FileContentTypes.createdAtHuman(record.getCreatedAt()),
                    new ArrayList<>(tags)));
        }

        Meta meta = new Meta(views.size(), filterApplied, executionTimeMs, orgId.toString());
        Summary summary =
                new Summary(views.size(), FileContentTypes.sizeHuman(totalSize), byCategory);

        return new SearchResponse(meta, summary, views);
    }

    private static String statusTag(String status) {
        String value = status == null ? "" : status.toUpperCase(Locale.ROOT);
        return switch (value) {
            case "AVAILABLE" -> "active";
            case "DELETED" -> "deleted";
            case "INFECTED" -> "infected";
            default -> "unknown";
        };
    }
}
