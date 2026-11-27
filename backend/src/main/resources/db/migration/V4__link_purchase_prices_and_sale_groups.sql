ALTER TABLE ticket_purchases ADD COLUMN quoted_price_id BIGINT REFERENCES meal_prices(id);
ALTER TABLE ticket_exchanges ADD COLUMN sale_group VARCHAR(50);
CREATE INDEX idx_ticket_exchanges_sale_group ON ticket_exchanges(sale_group) WHERE sale_group IS NOT NULL;
ALTER TABLE ticket_exchange_transactions ADD COLUMN amount NUMERIC(12,2) CHECK (amount >= 0);
