package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminReportDetail(LocalDate from, LocalDate to, List<Department> departments,
        List<Employee> employees, List<Ticket> tickets, List<Transfer> transactions) {
    public record Department(long id, String name) {}
    public record Employee(long id, String code, String name, long departmentId) {}
    public record Ticket(long id, String code, long userId, LocalDate date, TicketType type, BigDecimal price,
            Long purchaseId, Long transferId) {}
    public record Transfer(long id, long fromId, long toId, LocalDate date, TicketType type,
            BigDecimal amount, int quantity, LocalDateTime createdAt) {}
}
