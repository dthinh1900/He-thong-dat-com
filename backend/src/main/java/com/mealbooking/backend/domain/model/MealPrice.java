package com.mealbooking.backend.domain.model;

import com.mealbooking.backend.domain.enums.TicketType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class MealPrice {
    private Long id;
    private TicketType ticketType;
    private BigDecimal price;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private User createdBy;
    private LocalDateTime createdAt;
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
