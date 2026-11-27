ALTER TABLE meal_tickets
    DROP CONSTRAINT meal_tickets_status_check,
    ADD CONSTRAINT meal_tickets_status_check CHECK (status IN ('PENDING_APPROVAL', 'AVAILABLE',
        'LISTED_FOR_EXCHANGE', 'USED', 'CANCELLED', 'REJECTED', 'WITHDRAWN'));

ALTER TABLE audit_logs
    DROP CONSTRAINT audit_logs_action_check,
    ADD CONSTRAINT audit_logs_action_check CHECK (action IN ('REGISTER_TICKET', 'CANCEL_TICKET',
        'REQUEST_PURCHASE', 'APPROVE_PURCHASE', 'REJECT_PURCHASE', 'CREATE_EXCHANGE', 'COMPLETE_EXCHANGE',
        'CANCEL_EXCHANGE', 'WITHDRAW_TICKET'));

-- Reclassify known self-removals of the original owner while keeping their audit and metadata.
UPDATE meal_tickets t SET status = 'WITHDRAWN'
FROM meal_registrations r
WHERE t.status = 'CANCELLED' AND t.cancellation_source = 'SELF'
    AND t.registration_id = r.id AND t.owner_id = r.user_id;
