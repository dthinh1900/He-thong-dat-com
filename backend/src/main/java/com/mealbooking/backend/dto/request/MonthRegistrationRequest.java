package com.mealbooking.backend.dto.request;

import com.mealbooking.backend.application.model.SaveMonthRegistration;
import com.mealbooking.backend.domain.enums.TicketType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record MonthRegistrationRequest(
    @Min(1) @Max(12) int month,
    @Min(2000) @Max(2200) int year,
    @NotNull List<LocalDate> selectedDates,
    Map<LocalDate, TicketType> selectedTypes
) {
    public SaveMonthRegistration toCommand() {
        return new SaveMonthRegistration(month, year, selectedDates, selectedTypes);
    }
}
