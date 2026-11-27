package com.mealbooking.backend.domain.model;

import com.mealbooking.backend.domain.enums.CancellationSource;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class MealTicket {
    private Long id;
    private String ticketCode;
    private MealRegistration registration;
    private TicketPurchase purchaseRequest;
    private User owner;
    private MealPrice mealPrice;
    private LocalDate mealDate;
    private TicketType ticketType;
    private BigDecimal price;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    private User cancelledBy;
    private CancellationSource cancellationSource;
    private String cancellationReason;
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
