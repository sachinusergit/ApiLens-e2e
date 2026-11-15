package com.apilens.file.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Content-type detection plus the presentation helpers used by the search API
 * (human sizes, categories, extension, preview flag, tags, human dates).
 */
public final class FileContentTypes {

    private static final Map<String, String> BY_EXTENSION = new LinkedHashMap<>();

    private static final DateTimeFormatter HUMAN_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);

    private static final Set<String> PREVIEWABLE = Set.of(
            "application/pdf",
            "application/json",
            "application/xml",
            "application/javascript",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "image/svg+xml");

    static {
        BY_EXTENSION.put("txt", "text/plain");
        BY_EXTENSION.put("text", "text/plain");
        BY_EXTENSION.put("log", "text/plain");
        BY_EXTENSION.put("md", "text/markdown");
        BY_EXTENSION.put("markdown", "text/markdown");
        BY_EXTENSION.put("csv", "text/csv");
        BY_EXTENSION.put("json", "application/json");
        BY_EXTENSION.put("xml", "application/xml");
        BY_EXTENSION.put("yaml", "application/x-yaml");
        BY_EXTENSION.put("yml", "application/x-yaml");
        BY_EXTENSION.put("html", "text/html");
        BY_EXTENSION.put("htm", "text/html");
        BY_EXTENSION.put("css", "text/css");
        BY_EXTENSION.put("js", "application/javascript");
        BY_EXTENSION.put("pdf", "application/pdf");
        BY_EXTENSION.put("png", "image/png");
        BY_EXTENSION.put("jpg", "image/jpeg");
        BY_EXTENSION.put("jpeg", "image/jpeg");
        BY_EXTENSION.put("gif", "image/gif");
        BY_EXTENSION.put("webp", "image/webp");
        BY_EXTENSION.put("svg", "image/svg+xml");
        BY_EXTENSION.put("bmp", "image/bmp");
        BY_EXTENSION.put("ico", "image/x-icon");
        BY_EXTENSION.put("doc", "application/msword");
        BY_EXTENSION.put("docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        BY_EXTENSION.put("xls", "application/vnd.ms-excel");
        BY_EXTENSION.put("xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        BY_EXTENSION.put("ppt", "application/vnd.ms-powerpoint");
        BY_EXTENSION.put("pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        BY_EXTENSION.put("odt", "application/vnd.oasis.opendocument.text");
        BY_EXTENSION.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
        BY_EXTENSION.put("rtf", "application/rtf");
        BY_EXTENSION.put("zip", "application/zip");
        BY_EXTENSION.put("rar", "application/vnd.rar");
        BY_EXTENSION.put("7z", "application/x-7z-compressed");
        BY_EXTENSION.put("tar", "application/x-tar");
        BY_EXTENSION.put("gz", "application/gzip");
        BY_EXTENSION.put("mp4", "video/mp4");
        BY_EXTENSION.put("mov", "video/quicktime");
        BY_EXTENSION.put("mp3", "audio/mpeg");
        BY_EXTENSION.put("wav", "audio/wav");
    }

    private FileContentTypes() {
    }

    /** Lower-case extension including the dot, or "" when there is none. */
    public static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    /**
     * Trusts the filename extension for known types; browsers often report
     * generic types such as application/octet-stream for .md files.
     */
    public static String normalize(String filename, String provided) {
        String ext = extension(filename);
        if (!ext.isEmpty()) {
            String mapped = BY_EXTENSION.get(ext.substring(1));
            if (mapped != null) {
                return mapped;
            }
        }
        if (provided == null || provided.isBlank()) {
            return "application/octet-stream";
        }
        return provided.toLowerCase(Locale.ROOT);
    }

    public static String category(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        if (ct.startsWith("image/")) {
            return "image";
        }
        if (ct.startsWith("video/")) {
            return "video";
        }
        if (ct.startsWith("audio/")) {
            return "audio";
        }
        if (ct.contains("zip") || ct.contains("rar") || ct.contains("7z")
                || ct.contains("tar") || ct.contains("gzip")) {
            return "archive";
        }
        if (ct.startsWith("text/") || ct.equals("application/pdf")
                || ct.equals("application/json") || ct.endsWith("+json")
                || ct.equals("application/xml") || ct.endsWith("+xml")
                || ct.contains("word") || ct.contains("document")
                || ct.contains("excel") || ct.contains("spreadsheet")
                || ct.contains("powerpoint") || ct.contains("presentation")
                || ct.equals("application/rtf") || ct.equals("application/javascript")) {
            return "document";
        }
        return "other";
    }

    public static boolean canPreview(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return ct.startsWith("text/") || PREVIEWABLE.contains(ct);
    }

    public static String sizeHuman(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.US, "%.1f MB", mb);
        }
        return String.format(Locale.US, "%.1f GB", mb / 1024.0);
    }

    public static String createdAtHuman(Instant instant) {
        return instant == null ? "" : HUMAN_DATE.format(instant);
    }

    public static boolean isToday(Instant instant) {
        return instant != null
                && instant.atZone(ZoneOffset.UTC).toLocalDate().equals(LocalDate.now(ZoneOffset.UTC));
    }
}
