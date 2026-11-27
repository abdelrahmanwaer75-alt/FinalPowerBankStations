-- The only manual admin-account script: creates/activates the sole SUPER_ADMIN,
-- 009ardh@gmail.com, and removes SUPER_ADMIN from every other account.
-- The password is stored as a BCrypt hash, never as plain text.
-- Run in MySQL Command Line Client with:
-- SOURCE G:/project/PowerBankStations/database/add_admin_009ardh.sql;

USE powerbank_stations;

START TRANSACTION;

INSERT INTO roles (name, description, is_enabled)
VALUES ('SUPER_ADMIN', 'Full access to every admin function', TRUE)
ON DUPLICATE KEY UPDATE is_enabled = TRUE;

INSERT INTO users (full_name, email, phone, password_hash, account_status, email_verified_at)
VALUES (
    'PowerBank Admin',
    '009ardh@gmail.com',
    NULL,
    '$2a$12$MBCTEPE6BqhurGSVfrd5u.PHWRDhQRPoLqDnbzJSNGDQC6XK0aVTa',
    'ACTIVE',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    full_name = 'PowerBank Admin',
    account_status = 'ACTIVE',
    email_verified_at = CURRENT_TIMESTAMP;

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE LOWER(u.email) = LOWER('009ardh@gmail.com')
  AND r.name = 'SUPER_ADMIN';

DELETE ur
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id AND r.name = 'SUPER_ADMIN'
JOIN users target ON LOWER(target.email) = LOWER('009ardh@gmail.com')
WHERE ur.user_id <> target.id;

COMMIT;

SELECT u.email, u.account_status, r.name AS role
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE LOWER(u.email) = LOWER('009ardh@gmail.com')
  AND r.name = 'SUPER_ADMIN';
