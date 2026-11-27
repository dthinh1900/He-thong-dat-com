LOCK TABLE meal_prices IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    effective_day DATE := (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;
    legacy_price RECORD;
BEGIN
    FOR legacy_price IN
        SELECT p.*, rates.new_price
        FROM meal_prices p
        JOIN (VALUES ('NORMAL', 30000, 25000), ('SPECIAL', 50000, 40000))
            AS rates(ticket_type, old_price, new_price)
            ON p.ticket_type = rates.ticket_type AND p.price = rates.old_price
        WHERE p.effective_to IS NULL OR p.effective_to >= effective_day
    LOOP
        IF legacy_price.effective_from >= effective_day THEN
            UPDATE meal_prices SET price = legacy_price.new_price WHERE id = legacy_price.id;
        ELSE
            UPDATE meal_prices SET effective_to = effective_day - 1 WHERE id = legacy_price.id;
            INSERT INTO meal_prices(ticket_type, price, effective_from, effective_to)
            VALUES (legacy_price.ticket_type, legacy_price.new_price, effective_day, legacy_price.effective_to);
        END IF;
    END LOOP;
END $$;
