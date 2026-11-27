-- Apply once to an existing powerbank_stations database.
USE powerbank_stations;

CREATE TABLE IF NOT EXISTS powerbank_status_history (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    powerbank_id    BIGINT UNSIGNED NOT NULL,
    old_status      VARCHAR(40) NULL,
    new_status      VARCHAR(40) NOT NULL,
    changed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pb_status_history_bank FOREIGN KEY (powerbank_id)
        REFERENCES powerbanks(id) ON DELETE RESTRICT,
    INDEX idx_pb_status_history_bank_time (powerbank_id, changed_at, id)
) ENGINE=InnoDB;

-- Record the current state as a baseline for devices created before this migration.
INSERT INTO powerbank_status_history (powerbank_id, old_status, new_status, changed_at)
SELECT p.id, NULL, p.status, COALESCE(p.created_at, CURRENT_TIMESTAMP)
FROM powerbanks p
WHERE NOT EXISTS (
    SELECT 1 FROM powerbank_status_history h WHERE h.powerbank_id = p.id
);

DROP TRIGGER IF EXISTS trg_powerbanks_status_insert;
DROP TRIGGER IF EXISTS trg_powerbanks_status_update;

DELIMITER $$
CREATE TRIGGER trg_powerbanks_status_insert
AFTER INSERT ON powerbanks
FOR EACH ROW
BEGIN
    INSERT INTO powerbank_status_history (powerbank_id, old_status, new_status)
    VALUES (NEW.id, NULL, NEW.status);
END$$

CREATE TRIGGER trg_powerbanks_status_update
AFTER UPDATE ON powerbanks
FOR EACH ROW
BEGIN
    IF NOT (OLD.status <=> NEW.status) THEN
        INSERT INTO powerbank_status_history (powerbank_id, old_status, new_status)
        VALUES (NEW.id, OLD.status, NEW.status);
    END IF;
END$$
DELIMITER ;
