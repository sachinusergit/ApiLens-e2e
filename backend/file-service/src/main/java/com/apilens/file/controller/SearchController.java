package com.apilens.file.controller;

import com.apilens.file.domain.FileRecord;
import com.apilens.file.dto.FileDto;
import com.apilens.file.dto.SearchResponse;
import com.apilens.file.repository.FileRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final FileRepository repo;

    public SearchController(FileRepository repo) {
        this.repo = repo;
    }

    public record SearchRequest(
            String contentType,
            String status,
            String dateFrom,
            String dateTo) {
    }

    // Feature 2: Checkbox filters (contentType) + Feature 3: Date range
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        String applied = filterApplied(contentType, status, dateFrom, dateTo);

        long started = System.nanoTime();
        List<FileRecord> records = runSearch(orgId, contentType, status, dateFrom, dateTo);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        return ResponseEntity.ok(SearchResponse.of(records, applied, orgId, elapsedMs));
    }

    // Body-based variant kept for clients that prefer a JSON payload.
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> searchPost(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestBody(required = false) SearchRequest body) {

        SearchRequest req = body == null ? new SearchRequest(null, null, null, null) : body;
        String applied =
                filterApplied(req.contentType(), req.status(), req.dateFrom(), req.dateTo());

        long started = System.nanoTime();
        List<FileRecord> records =
                runSearch(orgId, req.contentType(), req.status(), req.dateFrom(), req.dateTo());
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        return ResponseEntity.ok(SearchResponse.of(records, applied, orgId, elapsedMs));
    }

    private List<FileRecord> runSearch(
            UUID orgId,
            String contentType,
            String status,
            String dateFrom,
            String dateTo) {

        Instant from = parseDate("dateFrom", dateFrom);
        Instant to = parseDate("dateTo", dateTo);

        boolean askingForDeleted = status != null && status.equalsIgnoreCase("DELETED");
        String type = contentType == null || contentType.isBlank()
                ? null
                : contentType.trim().toLowerCase(Locale.ROOT);
        String statusValue = status == null || status.isBlank()
                ? null
                : status.trim().toUpperCase(Locale.ROOT);

        Specification<FileRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("orgId"), orgId));

            // Deleted rows are hidden by default; the DELETED checkbox opts into them.
            if (!askingForDeleted) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("contentType"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (statusValue != null) {
                predicates.add(cb.equal(cb.upper(root.get("status")), statusValue));
            }

            if (query.getResultType() != Long.class) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<FileRecord> results = repo.findAll(spec);

        log.info("[FileSearch] contentType={} status={} dateFrom={} dateTo={} -> {} result(s)",
                contentType, status, dateFrom, dateTo, results.size());

        return results;
    }

    private static String filterApplied(
            String contentType, String status, String dateFrom, String dateTo) {

        List<String> parts = new ArrayList<>();
        if (contentType != null && !contentType.isBlank()) {
            parts.add("contentType=" + contentType.trim());
        }
        if (status != null && !status.isBlank()) {
            parts.add("status=" + status.trim());
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            parts.add("dateFrom=" + dateFrom.trim());
        }
        if (dateTo != null && !dateTo.isBlank()) {
            parts.add("dateTo=" + dateTo.trim());
        }
        return parts.isEmpty() ? "none" : String.join("&", parts);
    }

    // Feature 6: Autosuggest / autocomplete
    @GetMapping("/autocomplete")
    public ResponseEntity<List<FileDto>> autocomplete(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestParam String q) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        List<FileDto> results = repo.autocomplete(orgId, q.trim())
                .stream().limit(10).map(FileDto::from).toList();

        return ResponseEntity.ok(results);
    }

    private Instant parseDate(String name, String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid " + name + " format, use ISO-8601 (e.g. 2026-01-15T00:00:00Z)");
        }
    }
}
