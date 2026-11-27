ALTER TABLE meal_tickets
    ADD COLUMN cancellation_source VARCHAR(10) CHECK (cancellation_source IN ('SELF', 'ADMIN')),
    ADD COLUMN cancellation_reason VARCHAR(500);
