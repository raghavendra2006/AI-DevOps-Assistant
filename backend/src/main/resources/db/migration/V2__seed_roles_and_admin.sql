-- ==========================================================
-- Flyway Migration V2: Seed Default Roles and Initial Admin
-- ==========================================================

-- 1. Insert Core Roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'Full administrative access across all DevOps operations, user roles, and security audit trails'),
    ('ROLE_ENGINEER', 'Standard engineer access to create, trigger, and inspect DevOps pipelines and diagnostics'),
    ('ROLE_VIEWER', 'Read-only viewer access to metrics, logs, and pipeline execution reports')
ON CONFLICT (name) DO NOTHING;

-- 2. Insert Default Super Admin User (Password: Admin@DevOps2026!)
INSERT INTO users (email, password_hash, first_name, last_name, is_enabled, is_email_verified)
VALUES (
    'admin@aidevops.io',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System',
    'Administrator',
    TRUE,
    TRUE
)
ON CONFLICT (email) DO NOTHING;

-- 3. Map Super Admin to ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@aidevops.io' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
