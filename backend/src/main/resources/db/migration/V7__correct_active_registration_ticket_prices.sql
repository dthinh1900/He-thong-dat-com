-- Align unconsumed calendar registrations with the correction in V5 and V6.
UPDATE meal_tickets AS ticket
SET price = current_price.price, meal_price_id = current_price.id
FROM meal_registrations AS registration,
     meal_prices AS current_price,
     (VALUES ('NORMAL', 30000, 25000), ('SPECIAL', 50000, 40000))
         AS standard(ticket_type, old_price, new_price)
WHERE ticket.registration_id = registration.id
    AND ticket.owner_id = registration.user_id
    AND ticket.status = 'AVAILABLE'
    AND ticket.meal_date >= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
    AND ticket.ticket_type = standard.ticket_type
    AND ticket.price = standard.old_price
    AND current_price.ticket_type = ticket.ticket_type
    AND current_price.price = standard.new_price
    AND current_price.effective_from <= ticket.meal_date
    AND (current_price.effective_to IS NULL OR current_price.effective_to >= ticket.meal_date)
    AND NOT EXISTS (SELECT 1 FROM ticket_exchange_transactions AS transfer WHERE transfer.ticket_id = ticket.id);
