package com.mealbooking.backend.dto.request;

import com.mealbooking.backend.application.model.AdminCommands;
import com.mealbooking.backend.domain.enums.Role;
import com.mealbooking.backend.domain.enums.TicketType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public final class AdminRequests {
    private AdminRequests() {}
    public record DepartmentInput(@NotBlank @Size(max=150) String name,
        @Size(max=500) String description, Boolean active) {
        public AdminCommands.DepartmentInput toCommand() {
            return new AdminCommands.DepartmentInput(name, description, active);
        }
    }
    public record EmployeeInput(@NotBlank @Size(max=50) String code,
        @NotBlank @Size(max=160) String name, @NotBlank @Size(max=80) String username,
        @Email @Size(max=160) String email, @Size(max=30) String phone,
        @NotNull @Positive Long departmentId, @NotNull Role role, Boolean active,
        @Size(min=8,max=72) String password) {
        public AdminCommands.EmployeeInput toCommand() {
            return new AdminCommands.EmployeeInput(code, name, username, email, phone, departmentId, role, active, password);
        }
    }
    public record ActiveInput(@NotNull Boolean active) {}
    public record RegistrationInput(@NotNull Boolean registered, @Size(max=500) String cancellationReason, Boolean withdrawal) {}
    public record CancellationInput(@Size(max=500) String reason, Boolean withdrawal) {}
    public record SettingsInput(@NotNull LocalTime cutoff, @NotNull LocalTime cancelDeadline) {}
    public record PriceInput(@NotNull TicketType ticketType,
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal price,
        @NotNull LocalDate effectiveFrom) {
        public AdminCommands.PriceInput toCommand() {
            return new AdminCommands.PriceInput(ticketType, price, effectiveFrom);
        }
    }
}
