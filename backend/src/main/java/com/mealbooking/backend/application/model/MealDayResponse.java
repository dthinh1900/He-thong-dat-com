package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealDayResponse(
    LocalDate mealDate,
    boolean selected,
    Long ticketId,
    String ticketCode,
    String ticketStatus,
    TicketType ticketType,
    BigDecimal price,
    BigDecimal normalPrice,
    BigDecimal specialPrice,
    boolean editable,
    boolean typeEditable,
    String lockReason,
    int ticketCount,
    BigDecimal ownedAmount,
    List<CancelledMealTicket> cancelledTickets
) { }
