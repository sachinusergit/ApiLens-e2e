-- Promo banners used by the auto-showing modal.
-- Idempotent: safe to re-run because each insert is guarded by title.

INSERT INTO banners (id, title, body, image_url, cta_url, is_active, priority)
SELECT 'b0000000-0000-4000-8000-000000000001'::uuid,
       '🎉 50% OFF Pro Plan',
       'Upgrade today and unlock advanced API testing, custom reports, and CI/CD integration!',
       NULL,
       'https://example.com/pro',
       true,
       100
WHERE NOT EXISTS (SELECT 1 FROM banners WHERE title = '🎉 50% OFF Pro Plan');

INSERT INTO banners (id, title, body, image_url, cta_url, is_active, priority)
SELECT 'b0000000-0000-4000-8000-000000000002'::uuid,
       '🚀 Try Our New Feature',
       'Bulk test execution - run 1000 tests in parallel!',
       NULL,
       'https://example.com/bulk',
       true,
       90
WHERE NOT EXISTS (SELECT 1 FROM banners WHERE title = '🚀 Try Our New Feature');

INSERT INTO banners (id, title, body, image_url, cta_url, is_active, priority)
SELECT 'b0000000-0000-4000-8000-000000000003'::uuid,
       '📊 Advanced Analytics',
       'See test trends, coverage, and failure patterns.',
       NULL,
       'https://example.com/analytics',
       true,
       80
WHERE NOT EXISTS (SELECT 1 FROM banners WHERE title = '📊 Advanced Analytics');
