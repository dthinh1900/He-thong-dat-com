-- Persist registration and cancellation cutoffs. Preserve any previously configured values.
CREATE TABLE IF NOT EXISTS registration_settings (
    id INTEGER PRIMARY KEY CHECK (id=1),
    cutoff TIME NOT NULL DEFAULT '14:45',
    cancel_deadline TIME NOT NULL DEFAULT '14:45'
);
INSERT INTO registration_settings(id) VALUES(1) ON CONFLICT(id) DO NOTHING;
