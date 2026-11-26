-- Preserve all planned/actual dates and historical invoices. Old rows remain NULL.
ALTER TABLE bookings
    ADD COLUMN checked_out_by_id BIGINT NULL,
    ADD COLUMN checkout_reason VARCHAR(500) NULL,
    ADD CONSTRAINT fk_booking_checkout_actor FOREIGN KEY (checked_out_by_id) REFERENCES users(id);
