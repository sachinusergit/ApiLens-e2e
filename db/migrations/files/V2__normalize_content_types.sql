-- ================================================================
-- apilens_files - V2
-- Browsers often report generic MIME types (application/octet-stream)
-- for text formats. Normalize already stored rows by extension so the
-- content-type filters (Text, Markdown, PDF, ...) match correctly.
-- ================================================================

UPDATE files
SET content_type = 'text/markdown', updated_at = now()
WHERE (content_type IS NULL OR content_type = 'application/octet-stream'
       OR content_type = 'text/x-markdown')
  AND (lower(original_name) LIKE '%.md' OR lower(original_name) LIKE '%.markdown');

UPDATE files
SET content_type = 'text/plain', updated_at = now()
WHERE content_type = 'application/octet-stream'
  AND (lower(original_name) LIKE '%.txt' OR lower(original_name) LIKE '%.log'
       OR lower(original_name) LIKE '%.text');

UPDATE files
SET content_type = 'text/csv', updated_at = now()
WHERE content_type = 'application/octet-stream' AND lower(original_name) LIKE '%.csv';

UPDATE files
SET content_type = 'application/json', updated_at = now()
WHERE content_type = 'application/octet-stream' AND lower(original_name) LIKE '%.json';

UPDATE files
SET content_type = 'application/pdf', updated_at = now()
WHERE content_type = 'application/octet-stream' AND lower(original_name) LIKE '%.pdf';

UPDATE files
SET content_type = 'image/png', updated_at = now()
WHERE content_type = 'application/octet-stream' AND lower(original_name) LIKE '%.png';

UPDATE files
SET content_type = 'image/jpeg', updated_at = now()
WHERE content_type = 'application/octet-stream'
  AND (lower(original_name) LIKE '%.jpg' OR lower(original_name) LIKE '%.jpeg');

UPDATE files
SET content_type = 'application/zip', updated_at = now()
WHERE content_type = 'application/octet-stream' AND lower(original_name) LIKE '%.zip';
