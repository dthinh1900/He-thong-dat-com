LOCK TABLE meal_prices IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    effective_day DATE := (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;
    standard RECORD;
    active_price meal_prices%ROWTYPE;
    next_start DATE;
BEGIN
    FOR standard IN
        SELECT * FROM (VALUES ('NORMAL', 30000, 25000), ('SPECIAL', 50000, 40000))
            AS defaults(ticket_type, old_price, new_price)
    LOOP
        SELECT * INTO active_price FROM meal_prices
        WHERE ticket_type = standard.ticket_type AND effective_from <= effective_day
            AND (effective_to IS NULL OR effective_to >= effective_day);

        IF NOT FOUND THEN
            SELECT min(effective_from) INTO next_start FROM meal_prices
            WHERE ticket_type = standard.ticket_type AND effective_from > effective_day;
            INSERT INTO meal_prices(ticket_type, price, effective_from, effective_to)
            VALUES (standard.ticket_type, standard.new_price, effective_day, next_start - 1);
        ELSIF active_price.price = standard.old_price THEN
            IF active_price.effective_from = effective_day THEN
                UPDATE meal_prices SET price = standard.new_price WHERE id = active_price.id;
            ELSE
                UPDATE meal_prices SET effective_to = effective_day - 1 WHERE id = active_price.id;
                INSERT INTO meal_prices(ticket_type, price, effective_from, effective_to)
                VALUES (standard.ticket_type, standard.new_price, effective_day, active_price.effective_to);
            END IF;
        END IF;
    END LOOP;
END $$;
