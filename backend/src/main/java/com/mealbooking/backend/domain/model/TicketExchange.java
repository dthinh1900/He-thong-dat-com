package com.mealbooking.backend.domain.model;

import com.mealbooking.backend.domain.enums.ExchangeStatus;
import com.mealbooking.backend.domain.enums.ExchangeType;
import com.mealbooking.backend.domain.enums.TicketType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class TicketExchange {
    private Long id;
    private String exchangeCode;
    private String saleGroup;
    private User requester;
    private ExchangeType exchangeType;
    private MealTicket ticket;
    private LocalDate mealDate;
    private TicketType ticketType;
    private Integer quantityRequested;
    private Integer quantityCompleted = 0;
    private ExchangeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
