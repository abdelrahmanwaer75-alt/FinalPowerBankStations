-- Run once on an existing MySQL database to allow multiple open reservations per customer.
-- The unique key on open_powerbank_key remains, so a PowerBank can still be reserved only once.
ALTER TABLE reservations
    DROP INDEX uq_reservations_open_user,
    DROP COLUMN open_user_key;
