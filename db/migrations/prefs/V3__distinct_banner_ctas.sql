-- Give every active promo banner its own destination.
-- Older seeded banners are deactivated so exactly four promos remain active.

UPDATE banners
SET cta_url = 'https://github.com/features/actions'
WHERE title = '🎉 50% OFF Pro Plan';

UPDATE banners
SET title = '🚀 Try Bulk Test Execution',
    body = 'Run 1000 API tests in parallel - 10x faster!',
    cta_url = 'https://k6.io/docs/'
WHERE title = '🚀 Try Our New Feature';

UPDATE banners
SET cta_url = 'https://grafana.com/'
WHERE title = '📊 Advanced Analytics';

UPDATE banners
SET is_active = false
WHERE title IN ('Upgrade to Pro', 'New Feature: Bulk Upload');

INSERT INTO banners (id, title, body, image_url, cta_url, is_active, priority)
SELECT 'b0000000-0000-4000-8000-000000000004'::uuid,
       '🔐 Enterprise SSO',
       'SAML, OAuth 2.0, OIDC - all supported.',
       NULL,
       'https://auth0.com/',
       true,
       70
WHERE NOT EXISTS (SELECT 1 FROM banners WHERE title = '🔐 Enterprise SSO');
