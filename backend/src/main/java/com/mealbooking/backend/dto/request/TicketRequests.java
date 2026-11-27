package com.mealbooking.backend.dto.request;

import com.mealbooking.backend.application.model.TicketCommands;
import com.mealbooking.backend.domain.enums.ExchangeType;
import com.mealbooking.backend.domain.enums.PurchaseStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class TicketRequests {
    private TicketRequests() {}

    public record PurchaseInput(
            @NotNull LocalDate mealDate,
            @NotNull TicketType ticketType,
            @NotNull @Min(1) @Max(30) Integer quantity,
            @NotBlank @Size(max = 1000) String reason,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal expectedUnitPrice) {
        public TicketCommands.PurchaseInput toCommand() {
            return new TicketCommands.PurchaseInput(mealDate, ticketType, quantity, reason, expectedUnitPrice);
        }
    }

    public record PurchaseDecision(@NotNull PurchaseStatus decision) {}

    public record ExchangeInput(
            ExchangeType exchangeType,
            @NotNull LocalDate mealDate,
            @NotNull TicketType ticketType,
            @NotNull @Min(1) @Max(30) Integer quantity) {
        public TicketCommands.ExchangeInput toCommand() {
            return new TicketCommands.ExchangeInput(exchangeType, mealDate, ticketType, quantity);
        }
    }

    public record ExchangeMatch(@Min(1) @Max(30) Integer quantity) {}
}
