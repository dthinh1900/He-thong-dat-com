package com.mealbooking.backend.domain.model;

import com.mealbooking.backend.domain.enums.PurchaseStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class TicketPurchase {
    private Long id;
    private String purchaseCode;
    private User requester;
    private LocalDate mealDate;
    private TicketType ticketType;
    private Integer quantity;
    private BigDecimal totalAmount;
    private MealPrice quotedPrice;
    private PurchaseStatus status;
    private String note;
    private User reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
