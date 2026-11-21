-- =====================================================================
-- PowerBank Stations - MySQL schema (CSE211)
-- Target: MySQL 8.0+ (validated on MariaDB 10.11 for syntax)
-- Run:    mysql -u root -p < database/schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS powerbank_stations;
CREATE DATABASE powerbank_stations
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE powerbank_stations;

-- ---------------------------------------------------------------------
-- 1. users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    full_name           VARCHAR(120)  NOT NULL,
    email               VARCHAR(190)  NOT NULL,
    phone               VARCHAR(20)   NULL,
    password_hash       VARCHAR(255)  NOT NULL,
    account_status      ENUM('PENDING_VERIFICATION','ACTIVE','SUSPENDED') NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified_at   TIMESTAMP     NULL,
    last_login_at       TIMESTAMP     NULL,
    profile_image_path  VARCHAR(255)  NULL,
    language_preference ENUM('AR','EN') NOT NULL DEFAULT 'EN',
    theme_preference    ENUM('LIGHT','DARK') NOT NULL DEFAULT 'LIGHT',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_email CHECK (email LIKE '%_@_%.__%')
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2-5. Roles, permissions, and their links
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    is_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE permissions (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(60)  NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT uq_permissions_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    role_id       INT UNSIGNED NOT NULL,
    permission_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id     BIGINT UNSIGNED NOT NULL,
    role_id     INT UNSIGNED    NOT NULL,
    assigned_by BIGINT UNSIGNED NULL,
    assigned_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user        FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role        FOREIGN KEY (role_id)     REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. stations
-- ---------------------------------------------------------------------
CREATE TABLE stations (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    address         VARCHAR(255)  NOT NULL,
    city            VARCHAR(100)  NOT NULL,
    latitude        DECIMAL(10,7) NOT NULL,
    longitude       DECIMAL(10,7) NOT NULL,
    total_capacity  INT UNSIGNED  NOT NULL DEFAULT 0,
    opening_hours   VARCHAR(120)  NULL,
    contact_phone   VARCHAR(20)   NULL,
    description     TEXT          NULL,
    image_path      VARCHAR(255)  NULL,
    status          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_stations_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_stations_lng CHECK (longitude BETWEEN -180 AND 180),
    INDEX idx_stations_status_city (status, city),
    INDEX idx_stations_location (latitude, longitude)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. powerbanks
-- ---------------------------------------------------------------------
CREATE TABLE powerbanks (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    serial_number       VARCHAR(60)  NOT NULL,
    model               VARCHAR(100) NULL,
    station_id          BIGINT UNSIGNED NULL,
    status              ENUM('AVAILABLE','RESERVED','RENTED','RETURNED_PENDING_REVIEW',
                             'UNDER_MAINTENANCE','UNAVAILABLE','RETIRED') NOT NULL DEFAULT 'AVAILABLE',
    registered_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maintenance_at TIMESTAMP    NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_powerbanks_serial UNIQUE (serial_number),
    CONSTRAINT fk_powerbanks_station FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE SET NULL,
    INDEX idx_powerbanks_station_status (station_id, status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. reservations
-- Generated key enforces one open reservation per PowerBank. A customer may
-- hold or rent multiple PowerBanks at the same time.
-- ---------------------------------------------------------------------
CREATE TABLE reservations (
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reference            VARCHAR(20)  NOT NULL,
    user_id              BIGINT UNSIGNED NOT NULL,
    station_id           BIGINT UNSIGNED NOT NULL,
    powerbank_id         BIGINT UNSIGNED NULL,
    status               ENUM('PENDING','CONFIRMED','ACTIVE','CANCELLED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    duration_minutes     INT UNSIGNED NOT NULL DEFAULT 15,
    expires_at           TIMESTAMP    NOT NULL,
    cancelled_at         TIMESTAMP    NULL,
    open_powerbank_key   BIGINT UNSIGNED AS (
                           CASE WHEN status IN ('PENDING','CONFIRMED','ACTIVE') THEN powerbank_id END
                         ) STORED,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_reservations_reference UNIQUE (reference),
    CONSTRAINT uq_reservations_open_powerbank UNIQUE (open_powerbank_key),
    CONSTRAINT fk_res_user      FOREIGN KEY (user_id)      REFERENCES users(id),
    CONSTRAINT fk_res_station   FOREIGN KEY (station_id)   REFERENCES stations(id),
    CONSTRAINT fk_res_powerbank FOREIGN KEY (powerbank_id) REFERENCES powerbanks(id),
    CONSTRAINT ck_res_duration CHECK (duration_minutes BETWEEN 5 AND 120),
    INDEX idx_res_user_status (user_id, status),
    INDEX idx_res_station_status (station_id, status),
    INDEX idx_res_expires (status, expires_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 9. waiting_list
-- ---------------------------------------------------------------------
CREATE TABLE waiting_list (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    station_id  BIGINT UNSIGNED NOT NULL,
    status      ENUM('WAITING','NOTIFIED','CANCELLED','FULFILLED') NOT NULL DEFAULT 'WAITING',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_waiting_user_station UNIQUE (user_id, station_id),
    CONSTRAINT fk_wait_user    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wait_station FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE CASCADE,
    INDEX idx_wait_station_status (station_id, status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 14. pricing_plans (created before rentals, which reference it)
-- ---------------------------------------------------------------------
CREATE TABLE pricing_plans (
    id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    base_fee        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    price_per_minute DECIMAL(10,6) NOT NULL,
    daily_max_fee   DECIMAL(10,2) NULL,
    free_minutes    INT UNSIGNED  NOT NULL DEFAULT 0,
    late_fee        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    reservation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active       BOOLEAN       NOT NULL DEFAULT FALSE,
    effective_from  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT UNSIGNED NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_pricing_base     CHECK (base_fee >= 0),
    CONSTRAINT ck_pricing_minute   CHECK (price_per_minute >= 0),
    CONSTRAINT ck_pricing_daily    CHECK (daily_max_fee IS NULL OR daily_max_fee >= price_per_minute),
    CONSTRAINT ck_pricing_late     CHECK (late_fee >= 0),
    CONSTRAINT ck_pricing_reserve  CHECK (reservation_fee >= 0),
    CONSTRAINT fk_pricing_creator  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 10. rentals
-- ---------------------------------------------------------------------
CREATE TABLE rentals (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reference           VARCHAR(20)  NOT NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    powerbank_id        BIGINT UNSIGNED NOT NULL,
    pickup_station_id   BIGINT UNSIGNED NOT NULL,
    reservation_id      BIGINT UNSIGNED NULL,
    pricing_plan_id     INT UNSIGNED    NULL,
    status              ENUM('ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    start_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time            TIMESTAMP    NULL,
    duration_minutes    INT UNSIGNED NULL,
    estimated_cost      DECIMAL(10,2) NULL,
    final_cost          DECIMAL(10,2) NULL,
    active_powerbank_key BIGINT UNSIGNED AS (
                           CASE WHEN status = 'ACTIVE' THEN powerbank_id END
                         ) STORED,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_rentals_reference UNIQUE (reference),
    CONSTRAINT uq_rentals_active_powerbank UNIQUE (active_powerbank_key),
    CONSTRAINT uq_rentals_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_rentals_user        FOREIGN KEY (user_id)           REFERENCES users(id),
    CONSTRAINT fk_rentals_powerbank   FOREIGN KEY (powerbank_id)      REFERENCES powerbanks(id),
    CONSTRAINT fk_rentals_pickup      FOREIGN KEY (pickup_station_id) REFERENCES stations(id),
    CONSTRAINT fk_rentals_reservation FOREIGN KEY (reservation_id)    REFERENCES reservations(id),
    CONSTRAINT fk_rentals_plan        FOREIGN KEY (pricing_plan_id)   REFERENCES pricing_plans(id) ON DELETE SET NULL,
    CONSTRAINT ck_rentals_cost CHECK (final_cost IS NULL OR final_cost >= 0),
    INDEX idx_rentals_user_status (user_id, status),
    INDEX idx_rentals_status_start (status, start_time)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 15. rental_pricing_snapshots
-- Copies the applicable pricing into each rental so later price changes
-- never rewrite historical costs.
-- ---------------------------------------------------------------------
CREATE TABLE rental_pricing_snapshots (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rental_id       BIGINT UNSIGNED NOT NULL,
    plan_name       VARCHAR(100)  NOT NULL,
    base_fee        DECIMAL(10,2) NOT NULL,
    price_per_minute DECIMAL(10,6) NOT NULL,
    daily_max_fee   DECIMAL(10,2) NULL,
    free_minutes    INT UNSIGNED  NOT NULL,
    late_fee        DECIMAL(10,2) NOT NULL,
    reservation_fee DECIMAL(10,2) NOT NULL,
    captured_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_snapshot_rental UNIQUE (rental_id),
    CONSTRAINT fk_snapshot_rental FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 11. returns
-- ---------------------------------------------------------------------
CREATE TABLE returns (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reference         VARCHAR(20)  NOT NULL,
    rental_id         BIGINT UNSIGNED NOT NULL,
    return_station_id BIGINT UNSIGNED NULL,
    status            ENUM('PENDING_REVIEW','APPROVED','REQUIRES_MANUAL_REVIEW','REJECTED')
                      NOT NULL DEFAULT 'PENDING_REVIEW',
    returned_at       TIMESTAMP    NULL,
    final_duration_minutes INT UNSIGNED NULL,
    final_cost        DECIMAL(10,2) NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_returns_reference UNIQUE (reference),
    CONSTRAINT uq_returns_rental UNIQUE (rental_id),
    CONSTRAINT fk_returns_rental  FOREIGN KEY (rental_id)         REFERENCES rentals(id),
    CONSTRAINT fk_returns_station FOREIGN KEY (return_station_id) REFERENCES stations(id),
    INDEX idx_returns_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 12. maintenance_requests and 13. maintenance_notes
-- ---------------------------------------------------------------------
CREATE TABLE maintenance_requests (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reference         VARCHAR(20)  NOT NULL,
    powerbank_id      BIGINT UNSIGNED NULL,
    station_id        BIGINT UNSIGNED NULL,
    issue_description TEXT         NOT NULL,
    priority          ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    status            ENUM('OPEN','ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    assigned_to       BIGINT UNSIGNED NULL,
    created_by        BIGINT UNSIGNED NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_maint_reference UNIQUE (reference),
    CONSTRAINT ck_maint_target CHECK (powerbank_id IS NOT NULL OR station_id IS NOT NULL),
    CONSTRAINT fk_maint_powerbank FOREIGN KEY (powerbank_id) REFERENCES powerbanks(id),
    CONSTRAINT fk_maint_station   FOREIGN KEY (station_id)   REFERENCES stations(id),
    CONSTRAINT fk_maint_assignee  FOREIGN KEY (assigned_to)  REFERENCES users(id)      ON DELETE SET NULL,
    CONSTRAINT fk_maint_creator   FOREIGN KEY (created_by)   REFERENCES users(id)      ON DELETE SET NULL,
    INDEX idx_maint_status_priority (status, priority)
) ENGINE=InnoDB;

CREATE TABLE maintenance_notes (
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    maintenance_request_id BIGINT UNSIGNED NOT NULL,
    author_id              BIGINT UNSIGNED NULL,
    note                   TEXT          NOT NULL,
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mnote_request FOREIGN KEY (maintenance_request_id) REFERENCES maintenance_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_mnote_author  FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_mnote_request (maintenance_request_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 16. inventory_transfers and 17. inventory_transfer_items
-- ---------------------------------------------------------------------
CREATE TABLE inventory_transfers (
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reference              VARCHAR(20)  NOT NULL,
    source_station_id      BIGINT UNSIGNED NOT NULL,
    destination_station_id BIGINT UNSIGNED NOT NULL,
    status                 ENUM('PENDING','CONFIRMED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    transfer_date          DATE         NOT NULL,
    notes                  VARCHAR(500) NULL,
    created_by             BIGINT UNSIGNED NULL,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_transfer_reference UNIQUE (reference),
    CONSTRAINT ck_transfer_stations CHECK (source_station_id <> destination_station_id),
    CONSTRAINT fk_transfer_source FOREIGN KEY (source_station_id)      REFERENCES stations(id),
    CONSTRAINT fk_transfer_dest   FOREIGN KEY (destination_station_id) REFERENCES stations(id),
    CONSTRAINT fk_transfer_user   FOREIGN KEY (created_by)             REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_transfer_status (status)
) ENGINE=InnoDB;

CREATE TABLE inventory_transfer_items (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transfer_id  BIGINT UNSIGNED NOT NULL,
    powerbank_id BIGINT UNSIGNED NOT NULL,
    CONSTRAINT uq_transfer_item UNIQUE (transfer_id, powerbank_id),
    CONSTRAINT fk_titem_transfer  FOREIGN KEY (transfer_id)  REFERENCES inventory_transfers(id) ON DELETE CASCADE,
    CONSTRAINT fk_titem_powerbank FOREIGN KEY (powerbank_id) REFERENCES powerbanks(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 18. inspection_images (manual uploads; metadata only, files on disk)
-- owner_type/owner_id point to the record the image belongs to; the
-- service layer checks that the owner exists.
-- ---------------------------------------------------------------------
CREATE TABLE inspection_images (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_type    ENUM('RETURN','STATION_INSPECTION','CIRCUIT_BOARD_RECORD') NOT NULL,
    owner_id      BIGINT UNSIGNED NOT NULL,
    file_path     VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(50)  NOT NULL,
    file_size     INT UNSIGNED NOT NULL,
    uploaded_by   BIGINT UNSIGNED NULL,
    uploaded_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_image_path UNIQUE (file_path),
    CONSTRAINT ck_image_size CHECK (file_size > 0 AND file_size <= 5242880),
    CONSTRAINT ck_image_mime CHECK (mime_type IN ('image/jpeg','image/png')),
    CONSTRAINT fk_image_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_image_owner (owner_type, owner_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 19. inspection_records (station infrastructure and circuit board)
-- ---------------------------------------------------------------------
CREATE TABLE inspection_records (
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    record_type            ENUM('STATION_INFRASTRUCTURE','CIRCUIT_BOARD') NOT NULL,
    station_id             BIGINT UNSIGNED NULL,
    powerbank_id           BIGINT UNSIGNED NULL,
    circuit_board_id       VARCHAR(60)  NULL,
    inspection_date        DATE         NOT NULL,
    technician_id          BIGINT UNSIGNED NULL,
    description            TEXT         NULL,
    status                 ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    maintenance_request_id BIGINT UNSIGNED NULL,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_inspection_target CHECK (station_id IS NOT NULL OR powerbank_id IS NOT NULL),
    CONSTRAINT fk_insp_station    FOREIGN KEY (station_id)             REFERENCES stations(id),
    CONSTRAINT fk_insp_powerbank  FOREIGN KEY (powerbank_id)           REFERENCES powerbanks(id),
    CONSTRAINT fk_insp_technician FOREIGN KEY (technician_id)          REFERENCES users(id)                ON DELETE SET NULL,
    CONSTRAINT fk_insp_maint      FOREIGN KEY (maintenance_request_id) REFERENCES maintenance_requests(id) ON DELETE SET NULL,
    INDEX idx_insp_type_status (record_type, status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 20. manual_reviews (return inspection decisions made by staff)
-- ---------------------------------------------------------------------
CREATE TABLE manual_reviews (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    return_id   BIGINT UNSIGNED NOT NULL,
    reviewer_id BIGINT UNSIGNED NULL,
    decision    ENUM('APPROVED','REJECTED','ADDITIONAL_INFO_REQUESTED','MANUAL_REVIEW') NOT NULL,
    notes       TEXT          NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_return   FOREIGN KEY (return_id)   REFERENCES returns(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)   ON DELETE SET NULL,
    INDEX idx_review_return (return_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 21. notifications
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    title      VARCHAR(150) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (user_id, is_read)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 22. audit_logs (append-only: UPDATE and DELETE are blocked by triggers)
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT UNSIGNED NULL,
    action        VARCHAR(80)  NOT NULL,
    entity_type   VARCHAR(60)  NOT NULL,
    entity_id     VARCHAR(60)  NULL,
    result        ENUM('SUCCESS','FAILED') NOT NULL,
    ip_address    VARCHAR(45)  NULL,
    details       JSON         NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_actor_time (actor_user_id, created_at),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_action_time (action, created_at)
) ENGINE=InnoDB;

DELIMITER $$
CREATE TRIGGER trg_audit_logs_no_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';
END$$

CREATE TRIGGER trg_audit_logs_no_delete
BEFORE DELETE ON audit_logs
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 23. system_settings
-- ---------------------------------------------------------------------
CREATE TABLE system_settings (
    id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    updated_by    BIGINT UNSIGNED NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_settings_key UNIQUE (setting_key),
    CONSTRAINT fk_settings_user FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 24. auth_tokens (added): verification codes and password reset tokens.
-- Only a hash of the code or token is stored, never the plain value.
-- ---------------------------------------------------------------------
CREATE TABLE auth_tokens (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    purpose     ENUM('VERIFY_ACCOUNT','RESET_PASSWORD') NOT NULL,
    token_hash  CHAR(64)  NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token_user_purpose (user_id, purpose)
) ENGINE=InnoDB;
