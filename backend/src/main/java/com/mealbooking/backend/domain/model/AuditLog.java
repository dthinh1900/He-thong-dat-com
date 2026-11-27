package com.mealbooking.backend.domain.model;

import com.mealbooking.backend.domain.enums.AuditAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class AuditLog {
    private Long id;
    private User actor;
    private AuditAction action;
    private MealTicket ticket;
    private TicketExchangeTransaction transaction;
    private String description;
    private LocalDateTime createdAt;
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
