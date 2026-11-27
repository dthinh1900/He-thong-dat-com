package com.mealbooking.backend.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class TicketExchangeTransaction {
    private Long id;
    private String transactionCode;
    private TicketExchange buyRequest;
    private TicketExchange sellRequest;
    private MealTicket ticket;
    private User seller;
    private User buyer;
    private BigDecimal amount;
    private LocalDateTime completedAt;
    void onCreate() { if (completedAt == null) completedAt = LocalDateTime.now(); }
}
