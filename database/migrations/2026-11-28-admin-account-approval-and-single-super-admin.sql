USE powerbank_stations;

INSERT INTO roles (name, description, is_enabled)
VALUES ('SUPER_ADMIN', 'Full access to every admin function', TRUE)
ON DUPLICATE KEY UPDATE is_enabled = TRUE;

INSERT INTO roles (name, description, is_enabled) VALUES
    ('ADMIN', 'Administrative access to network operations', TRUE),
    ('STATION_MANAGER', 'Station operations access', TRUE)
ON DUPLICATE KEY UPDATE is_enabled = TRUE;

CREATE TABLE IF NOT EXISTS admin_account_requests (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    full_name          VARCHAR(120) NOT NULL,
    email              VARCHAR(190) NOT NULL,
    phone              VARCHAR(20) NULL,
    requested_role     ENUM('ADMIN','STATION_MANAGER') NOT NULL,
    status             ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    pending_email      VARCHAR(190) GENERATED ALWAYS AS (
                           CASE WHEN status = 'PENDING' THEN email ELSE NULL END
                       ) STORED,
    requested_by       BIGINT UNSIGNED NOT NULL,
    reviewed_by        BIGINT UNSIGNED NULL,
    created_user_id    BIGINT UNSIGNED NULL,
    review_notes       VARCHAR(500) NULL,
    requested_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at        TIMESTAMP NULL,
    CONSTRAINT uq_admin_request_pending_email UNIQUE (pending_email),
    CONSTRAINT uq_admin_request_created_user UNIQUE (created_user_id),
    CONSTRAINT fk_admin_requester FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT fk_admin_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_admin_request_user FOREIGN KEY (created_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_admin_request_status_date (status, requested_at)
) ENGINE=InnoDB;

-- Adopt the named Super Admin account and revoke SUPER_ADMIN from other users.
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u JOIN roles r ON r.name = 'SUPER_ADMIN'
WHERE LOWER(u.email) = LOWER('009ardh@gmail.com') AND u.account_status = 'ACTIVE';

DELETE ur
FROM user_roles ur
JOIN roles r ON r.id = ur.role_id AND r.name = 'SUPER_ADMIN'
WHERE ur.user_id <> COALESCE((
    SELECT target.id FROM users target
    WHERE LOWER(target.email) = LOWER('009ardh@gmail.com') AND target.account_status = 'ACTIVE'
    LIMIT 1
), 0);

DROP TRIGGER IF EXISTS trg_user_roles_super_admin_insert;
DROP TRIGGER IF EXISTS trg_user_roles_super_admin_update;

DELIMITER $$
CREATE TRIGGER trg_user_roles_super_admin_insert
BEFORE INSERT ON user_roles
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM roles r JOIN users u ON u.id = NEW.user_id
        WHERE r.id = NEW.role_id AND r.name = 'SUPER_ADMIN'
          AND LOWER(u.email) <> LOWER('009ardh@gmail.com')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only 009ardh@gmail.com can be SUPER_ADMIN';
    END IF;
END$$

CREATE TRIGGER trg_user_roles_super_admin_update
BEFORE UPDATE ON user_roles
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM roles r JOIN users u ON u.id = NEW.user_id
        WHERE r.id = NEW.role_id AND r.name = 'SUPER_ADMIN'
          AND LOWER(u.email) <> LOWER('009ardh@gmail.com')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only 009ardh@gmail.com can be SUPER_ADMIN';
    END IF;
END$$
DELIMITER ;
