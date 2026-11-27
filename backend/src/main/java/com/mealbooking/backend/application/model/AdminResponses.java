package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.Role;
import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class AdminResponses {
    private AdminResponses() {}
    public record DepartmentRow(Long id, String name, String description, boolean active, long employeeCount) {}
    public record EmployeeRow(Long id, String code, String name, String username, Long departmentId,
        String email, String phone, Role role, boolean active) {}
    public record Overview(LocalDate today, LocalDate tomorrow, long registeredToday, long registeredTomorrow,
        long employeeCount, long departmentCount, long pendingPurchases, boolean menuMissing) {}
    public record PortionCounts(long normal, long special) {}
    public record KitchenReport(PortionCounts counts, LocalDateTime reportedAt) {}
    public record KitchenCounts(LocalDate date, PortionCounts current, KitchenReport reported) {}
    public record RegistrationRow(Long id, String code, String name, Long departmentId, boolean active,
        boolean registered, Long ticketId, String status, boolean editable, String lockReason, boolean withdrawable) {}
    public record Settings(LocalTime cutoff, LocalTime cancelDeadline) {}
    public record PriceRow(Long id, TicketType ticketType, BigDecimal price, LocalDate effectiveFrom, LocalDate effectiveTo) {}
    public record MenuRow(Long id, LocalDate weekStartDate, LocalDate weekEndDate, String imageUrl, LocalDateTime uploadedAt) {}
    public record EmployeeTotal(Long id, String code, String name, long tickets, BigDecimal totalAmount) {}
    public record DepartmentTotal(Long id, String name, long tickets, BigDecimal totalAmount, List<EmployeeTotal> employees) {}
    public record DailyTotal(LocalDate date, long tickets, BigDecimal totalAmount) {}
    public record Report(int month, int year, long totalTickets, BigDecimal totalAmount,
        List<DepartmentTotal> departments, List<DailyTotal> days) {}
}
