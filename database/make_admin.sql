-- Gives an existing account the SUPER_ADMIN role.
-- 1) Register the account in the app first (POST /api/auth/register or the sign-up page).
-- 2) Replace the email below, then run:
--    mysql -u root -p powerbank_stations < database/make_admin.sql
USE powerbank_stations;

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'SUPER_ADMIN'
WHERE u.email = 'admin@example.com';

SELECT u.email, r.name AS role
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = 'admin@example.com';
