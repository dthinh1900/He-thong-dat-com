package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.Role;
import com.mealbooking.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class AdminCommands {
    private AdminCommands() {}

    public record DepartmentInput(String name, String description, Boolean active) {}

    public record EmployeeInput(String code, String name, String username, String email, String phone,
            Long departmentId, Role role, Boolean active, String password) {}

    public record ImportRow(int row, EmployeeInput employee) {}

    public record PriceInput(TicketType ticketType, BigDecimal price, LocalDate effectiveFrom) {}
}
