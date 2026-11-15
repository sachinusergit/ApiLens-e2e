-- ================================================================
-- Demo seed — runs after migrations, populates 1 org + 1 user
-- Password for ravi@example.com is: Test@1234  (BCrypt, cost 10)
-- ================================================================

INSERT INTO organizations (id, name, slug)
VALUES ('00000000-0000-0000-0000-000000000001', 'Demo Org', 'demo-org')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO users (id, email, password_hash, name, org_id, role, email_verified)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'ravi@example.com',
    '$2a$10$Wjub.Lh4nXBuev15p8UDfuFtzNk2CmWOYxO0y0YMwbmaDgQXzHIm2',
    'Ravi',
    '00000000-0000-0000-0000-000000000001',
    'OWNER',
    true
)
ON CONFLICT DO NOTHING;
