-- Run with the Spring Boot backend stopped. This preserves the old hourly price
-- by converting it to its equivalent per-minute rate and removes per-user
-- uniqueness limits for active rentals and reservations.

-- The earlier reservation migration may already have been applied, so drop its
-- unique key/column only when they are still present.
SET @pb_drop_open_reservation_user := IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'reservations'
             AND index_name = 'uq_reservations_open_user'),
    'ALTER TABLE reservations DROP INDEX uq_reservations_open_user',
    'SELECT 1'
);
PREPARE pb_stmt FROM @pb_drop_open_reservation_user;
EXECUTE pb_stmt;
DEALLOCATE PREPARE pb_stmt;

SET @pb_drop_open_user_column := IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'reservations'
             AND column_name = 'open_user_key'),
    'ALTER TABLE reservations DROP COLUMN open_user_key',
    'SELECT 1'
);
PREPARE pb_stmt FROM @pb_drop_open_user_column;
EXECUTE pb_stmt;
DEALLOCATE PREPARE pb_stmt;

ALTER TABLE rentals
    DROP INDEX uq_rentals_active_user,
    DROP COLUMN active_user_key;

ALTER TABLE pricing_plans
    DROP CHECK ck_pricing_hour,
    DROP CHECK ck_pricing_daily;

ALTER TABLE pricing_plans
    CHANGE COLUMN price_per_hour price_per_minute DECIMAL(10,6) NOT NULL;

UPDATE pricing_plans
SET price_per_minute = ROUND(price_per_minute / 60, 6);

ALTER TABLE pricing_plans
    ADD CONSTRAINT ck_pricing_minute CHECK (price_per_minute >= 0),
    ADD CONSTRAINT ck_pricing_daily CHECK (daily_max_fee IS NULL OR daily_max_fee >= price_per_minute);

ALTER TABLE rental_pricing_snapshots
    CHANGE COLUMN price_per_hour price_per_minute DECIMAL(10,6) NOT NULL;

UPDATE rental_pricing_snapshots
SET price_per_minute = ROUND(price_per_minute / 60, 6);