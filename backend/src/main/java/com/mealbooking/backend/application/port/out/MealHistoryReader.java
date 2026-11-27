package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.TicketResponses.MealHistorySource;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MealHistoryReader {
    List<OwnedTicket> findOwnedTickets(Long ownerId, LocalDate from, LocalDate to);

    record OwnedTicket(long id, LocalDate mealDate, TicketType type, BigDecimal price,
                       TicketStatus status, MealHistorySource source) {}
}
