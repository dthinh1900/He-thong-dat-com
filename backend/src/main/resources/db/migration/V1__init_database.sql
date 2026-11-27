-- Initial schema: ten core tables.
-- Also supports first-time Flyway adoption of databases previously initialized manually.
-- Existing tables and rows are preserved. Incompatible existing columns are not repaired.
-- Flyway owns the transaction; do not add BEGIN/COMMIT.

CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    username VARCHAR(80) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(160),
    phone VARCHAR(30),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_department ON users(department_id);

CREATE TABLE IF NOT EXISTS meal_registrations (
    id BIGSERIAL PRIMARY KEY,
    registration_code VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INTEGER NOT NULL CHECK (year BETWEEN 2000 AND 2200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_registration_user_month UNIQUE (user_id, month, year)
);

CREATE TABLE IF NOT EXISTS meal_prices (
    id BIGSERIAL PRIMARY KEY,
    ticket_type VARCHAR(20) NOT NULL CHECK (ticket_type IN ('NORMAL', 'SPECIAL')),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_meal_price_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT uq_meal_price_start UNIQUE (ticket_type, effective_from)
);
CREATE INDEX IF NOT EXISTS idx_meal_prices_lookup ON meal_prices(ticket_type, effective_from DESC);

DO $repair_function$
BEGIN
    IF to_regprocedure('public.prevent_meal_price_overlap()') IS NULL THEN
        EXECUTE $definition$CREATE FUNCTION prevent_meal_price_overlap() RETURNS trigger AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM meal_prices p
        WHERE p.ticket_type = NEW.ticket_type
          AND p.id <> COALESCE(NEW.id, -1)
          AND (NEW.effective_to IS NULL OR p.effective_from <= NEW.effective_to)
          AND (p.effective_to IS NULL OR NEW.effective_from <= p.effective_to)
    ) THEN
        RAISE EXCEPTION 'Khoảng hiệu lực giá vé bị chồng lấn cho loại %', NEW.ticket_type;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;$definition$;
    END IF;
END;
$repair_function$;

DO $repair_trigger$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid = 'public.meal_prices'::regclass AND tgname = 'trg_meal_price_no_overlap' AND NOT tgisinternal) THEN
        CREATE TRIGGER trg_meal_price_no_overlap
        BEFORE INSERT OR UPDATE OF ticket_type, effective_from, effective_to ON meal_prices
        FOR EACH ROW EXECUTE FUNCTION prevent_meal_price_overlap();
    END IF;
END;
$repair_trigger$;

CREATE TABLE IF NOT EXISTS ticket_purchases (
    id BIGSERIAL PRIMARY KEY,
    purchase_code VARCHAR(50) NOT NULL UNIQUE,
    requester_id BIGINT NOT NULL REFERENCES users(id),
    meal_date DATE NOT NULL,
    ticket_type VARCHAR(20) NOT NULL CHECK (ticket_type IN ('NORMAL', 'SPECIAL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    total_amount NUMERIC(14,2) CHECK (total_amount IS NULL OR total_amount >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    note VARCHAR(1000),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ticket_purchases_requester ON ticket_purchases(requester_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ticket_purchases_status ON ticket_purchases(status);

CREATE TABLE IF NOT EXISTS meal_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL UNIQUE,
    registration_id BIGINT REFERENCES meal_registrations(id),
    purchase_request_id BIGINT REFERENCES ticket_purchases(id),
    owner_id BIGINT NOT NULL REFERENCES users(id),
    meal_price_id BIGINT NOT NULL REFERENCES meal_prices(id),
    meal_date DATE NOT NULL,
    ticket_type VARCHAR(20) NOT NULL CHECK (ticket_type IN ('NORMAL', 'SPECIAL')),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING_APPROVAL', 'AVAILABLE', 'LISTED_FOR_EXCHANGE', 'USED', 'CANCELLED', 'REJECTED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancelled_by BIGINT REFERENCES users(id),
    CONSTRAINT ck_ticket_source CHECK (
        (registration_id IS NOT NULL AND purchase_request_id IS NULL)
        OR (registration_id IS NULL AND purchase_request_id IS NOT NULL)
    ),
    CONSTRAINT uq_registration_meal_date UNIQUE (registration_id, meal_date)
);
CREATE INDEX IF NOT EXISTS idx_meal_tickets_owner_date ON meal_tickets(owner_id, meal_date);
CREATE INDEX IF NOT EXISTS idx_meal_tickets_status_date ON meal_tickets(status, meal_date);

CREATE TABLE IF NOT EXISTS ticket_exchanges (
    id BIGSERIAL PRIMARY KEY,
    exchange_code VARCHAR(50) NOT NULL UNIQUE,
    requester_id BIGINT NOT NULL REFERENCES users(id),
    exchange_type VARCHAR(20) NOT NULL CHECK (exchange_type IN ('BUY', 'SELL')),
    ticket_id BIGINT REFERENCES meal_tickets(id),
    meal_date DATE NOT NULL,
    ticket_type VARCHAR(20) NOT NULL CHECK (ticket_type IN ('NORMAL', 'SPECIAL')),
    quantity_requested INTEGER NOT NULL CHECK (quantity_requested > 0),
    quantity_completed INTEGER NOT NULL DEFAULT 0 CHECK (quantity_completed >= 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'PARTIALLY_FILLED', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT ck_exchange_ticket_type CHECK (
        (exchange_type = 'SELL' AND ticket_id IS NOT NULL AND quantity_requested = 1)
        OR (exchange_type = 'BUY' AND ticket_id IS NULL)
    ),
    CONSTRAINT ck_exchange_quantity CHECK (quantity_completed <= quantity_requested)
);
CREATE INDEX IF NOT EXISTS idx_ticket_exchanges_status_date ON ticket_exchanges(status, meal_date);
CREATE INDEX IF NOT EXISTS idx_ticket_exchanges_requester ON ticket_exchanges(requester_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ticket_exchange_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_code VARCHAR(50) NOT NULL UNIQUE,
    buy_request_id BIGINT REFERENCES ticket_exchanges(id),
    sell_request_id BIGINT REFERENCES ticket_exchanges(id),
    ticket_id BIGINT NOT NULL REFERENCES meal_tickets(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_exchange_transactions_ticket ON ticket_exchange_transactions(ticket_id, completed_at DESC);

CREATE TABLE IF NOT EXISTS menu_images (
    id BIGSERIAL PRIMARY KEY,
    week_start_date DATE NOT NULL UNIQUE,
    week_end_date DATE NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    uploaded_by BIGINT NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_menu_week CHECK (week_end_date >= week_start_date)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(40) NOT NULL CHECK (action IN (
        'REGISTER_TICKET', 'CANCEL_TICKET', 'REQUEST_PURCHASE', 'APPROVE_PURCHASE',
        'REJECT_PURCHASE', 'CREATE_EXCHANGE', 'COMPLETE_EXCHANGE', 'CANCEL_EXCHANGE'
    )),
    ticket_id BIGINT REFERENCES meal_tickets(id),
    transaction_id BIGINT REFERENCES ticket_exchange_transactions(id),
    description VARCHAR(1500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_actor_created ON audit_logs(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_ticket ON audit_logs(ticket_id);
