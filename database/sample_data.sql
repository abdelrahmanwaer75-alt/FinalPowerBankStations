-- =====================================================================
-- PowerBank Stations - sample data
-- Run AFTER schema.sql:  mysql -u root -p powerbank_stations < database/sample_data.sql
-- No user accounts are inserted here. Create the first Super Admin with the
-- seed command in the backend (see README), which stores a BCrypt hash.
-- =====================================================================
USE powerbank_stations;

-- Default roles
INSERT INTO roles (name, description) VALUES
 ('SUPER_ADMIN',       'Full access to every admin function'),
 ('ADMIN',             'Manages stations, devices, and customers'),
 ('STATION_MANAGER',   'Manages stations, inventory, and returns'),
 ('MAINTENANCE_STAFF', 'Handles maintenance requests and inspections'),
 ('CUSTOMER',          'Regular customer account');

-- Permissions (codes used by the backend's @PreAuthorize checks)
INSERT INTO permissions (code, description) VALUES
 ('DASHBOARD_VIEW',      'View admin dashboard'),
 ('STATIONS_MANAGE',     'Manage stations'),
 ('POWERBANKS_MANAGE',   'Manage PowerBank devices'),
 ('INVENTORY_MANAGE',    'Manage inventory and transfers'),
 ('RESERVATIONS_MANAGE', 'Manage reservations'),
 ('RENTALS_MANAGE',      'Manage rentals'),
 ('RETURNS_MANAGE',      'Manage returns and manual reviews'),
 ('PRICING_MANAGE',      'Manage pricing plans'),
 ('CUSTOMERS_MANAGE',    'Manage customer accounts'),
 ('MAINTENANCE_MANAGE',  'Manage maintenance and inspections'),
 ('REPORTS_VIEW',        'View reports'),
 ('ROLES_MANAGE',        'Manage roles and permissions'),
 ('SETTINGS_MANAGE',     'Manage system settings'),
 ('AUDIT_VIEW',          'View audit logs');

-- Super Admin gets every permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN';

-- Admin: everything except roles and settings
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
 WHERE r.name = 'ADMIN' AND p.code NOT IN ('ROLES_MANAGE','SETTINGS_MANAGE');

-- Station manager
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
 WHERE r.name = 'STATION_MANAGER'
   AND p.code IN ('DASHBOARD_VIEW','STATIONS_MANAGE','POWERBANKS_MANAGE','INVENTORY_MANAGE','RETURNS_MANAGE','RESERVATIONS_MANAGE');

-- Maintenance staff
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
 WHERE r.name = 'MAINTENANCE_STAFF'
   AND p.code IN ('DASHBOARD_VIEW','MAINTENANCE_MANAGE','POWERBANKS_MANAGE');

-- System settings
INSERT INTO system_settings (setting_key, setting_value) VALUES
 ('app.name',                    'PowerBank Stations'),
 ('app.default_language',        'EN'),
 ('app.default_theme',           'LIGHT'),
 ('app.contact_email',           'support@powerbank.example'),
 ('app.contact_phone',           '+20 100 000 0000'),
 ('reservation.expiry_minutes',  '15'),
 ('upload.max_file_mb',          '5'),
 ('upload.max_files_per_return', '6');

-- Default active pricing plan
INSERT INTO pricing_plans
 (name, base_fee, price_per_hour, daily_max_fee, free_minutes, late_fee, reservation_fee, is_active)
VALUES
 ('Standard', 0.00, 8.00, 40.00, 5, 10.00, 0.00, TRUE);

-- Sample stations (coordinates are real city locations in Egypt)
INSERT INTO stations
 (name, address, city, latitude, longitude, total_capacity, opening_hours, contact_phone, description, status)
VALUES
 ('Central Station',       'Talaat Harb St',              'Cairo',     30.0444000, 31.2357000, 4, '24 hours',       '+20 100 000 0001', 'Near the main square', 'ACTIVE'),
 ('Zamalek Cafe Corner',   '26th of July St',             'Cairo',     30.0610000, 31.2190000, 4, '08:00 - 23:00',  '+20 100 000 0002', 'Opposite the metro',   'ACTIVE'),
 ('Nasr City Mall',        'Abbas El Akkad St',           'Cairo',     30.0626000, 31.3300000, 4, '10:00 - 00:00',  '+20 100 000 0003', 'Ground floor entrance', 'ACTIVE'),
 ('Maadi Road 9',          'Road 9',                      'Cairo',     29.9602000, 31.2569000, 4, '09:00 - 22:00',  '+20 100 000 0004', 'Near the tram stop',   'ACTIVE'),
 ('Alamein Campus',        'Alamein International Univ.', 'New Alamein', 30.8397000, 28.9550000, 4, '07:00 - 21:00', '+20 100 000 0005', 'Main gate',             'ACTIVE'),
 ('Marina Boardwalk',      'Marina 1',                    'North Coast', 30.8300000, 28.9400000, 4, '10:00 - 02:00', '+20 100 000 0006', 'Beach side',            'ACTIVE'),
 ('Giza Pyramids Road',    'Pyramids Road',               'Giza',      29.9870000, 31.2118000, 4, '08:00 - 20:00',  '+20 100 000 0007', 'Visitor parking',      'ACTIVE'),
 ('Heliopolis Korba',      'Korba St',                    'Cairo',     30.0900000, 31.3250000, 4, '09:00 - 23:00',  '+20 100 000 0008', 'Market area',          'ACTIVE'),
 ('Sheikh Zayed Mall',     'Sheikh Zayed City Mall',      'Giza',      30.0130000, 30.9900000, 4, '10:00 - 00:00',  '+20 100 000 0009', 'Food court level',     'ACTIVE'),
 ('Old Station Closed',    'Old Market',                  'Cairo',     30.0500000, 31.2400000, 4, '-',              NULL,               'Temporarily closed',  'INACTIVE');

-- 4 PowerBanks per station (serials are unique per station)
INSERT INTO powerbanks (serial_number, model, station_id, status, registered_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 4
)
SELECT CONCAT('PB-', LPAD(s.id, 3, '0'), '-', LPAD(seq.n, 2, '0')),
       'PowerBank 10000mAh',
       s.id,
       'AVAILABLE',
       NOW()
FROM stations s CROSS JOIN seq;
