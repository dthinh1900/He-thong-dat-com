package com.mealbooking.backend.application.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record MonthRegistrationResponse(
    int month,
    int year,
    int selectedCount,
    BigDecimal totalAmount,
    LocalTime cutoffTime,
    LocalTime cancelDeadline,
    List<MealDayResponse> days
) { }
