package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class TicketResponses {
    private TicketResponses() {}

    public record PurchaseQuote(BigDecimal unitPrice) {}

    public record PurchaseRow(Long id, String code, Long userId, String userName, LocalDate mealDate,
        TicketType ticketType, int quantity, BigDecimal totalAmount, PurchaseStatus status,
        String reason, LocalDateTime createdAt, LocalDateTime reviewedAt) {}

    public record ExchangeRow(Long id, String code, Long userId, String userName, ExchangeType side,
        LocalDate mealDate, TicketType ticketType, int quantity, int completed, int remaining,
        ExchangeStatus status, Long ticketId, BigDecimal price, LocalDateTime createdAt,
        List<BigDecimal> remainingPrices) {}

    public record TransactionRow(Long id, String code, Long sellerId, String sellerName,
        Long buyerId, String buyerName, Long ticketId, LocalDate mealDate, TicketType ticketType,
        BigDecimal amount, LocalDateTime completedAt) {}

    public record ExchangeBoard(List<ExchangeRow> offers, List<ExchangeRow> mine,
        List<TransactionRow> transactions, List<OwnedTicket> ownedTickets) {}

    public record OwnedTicket(Long id, LocalDate mealDate, TicketType ticketType, BigDecimal price,
        TicketStatus status) {}

    public record RosterRow(Long userId, String employeeCode, String name, Long departmentId,
        String departmentName, TicketType ticketType, long quantity) {}

    public record Roster(LocalDate date, long registeredEmployees, long totalTickets,
        List<RosterRow> rows) {}

    public record EmployeeTicket(Long ticketId, LocalDate mealDate, TicketType ticketType,
        BigDecimal price, TicketStatus status, boolean cancellable, boolean withdrawable) {}

    public record EmployeeHistory(Long userId, int month, int year, long totalTickets,
        BigDecimal totalAmount, List<EmployeeTicket> tickets) {}

    public enum MealHistorySource { REGISTRATION, PURCHASE, TRANSFER, OTHER }

    public record MealHistoryEntry(long ticketId, LocalDate mealDate, TicketType ticketType,
        BigDecimal price, TicketStatus status, MealHistorySource source, boolean billable) {}

    public record PersonalMealHistory(int month, int year, long totalTickets, BigDecimal totalAmount,
        List<MealHistoryEntry> tickets) {}

    public record DepartmentChoice(Long id, String name) {}

    public record ExchangeAuditRow(Long id, AuditAction action, Long actorId, String actorName,
        String description, LocalDateTime createdAt, Long ticketId, Long transactionId) {}
}
