package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.CancellationSource;
import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancelledMealTicket(Long ticketId, String ticketCode, TicketType ticketType, BigDecimal price,
        LocalDateTime cancelledAt, CancellationSource source, String reason) {}
