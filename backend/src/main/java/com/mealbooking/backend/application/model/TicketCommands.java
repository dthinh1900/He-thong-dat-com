package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.ExchangeType;
import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class TicketCommands {
    private TicketCommands() {}

    public record PurchaseInput(LocalDate mealDate, TicketType ticketType, int quantity, String reason,
            BigDecimal expectedUnitPrice) {}

    public record ExchangeInput(ExchangeType exchangeType, LocalDate mealDate, TicketType ticketType, int quantity) {}
}
