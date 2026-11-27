package com.mealbooking.backend.application.model;

import com.mealbooking.backend.domain.enums.TicketType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SaveMonthRegistration(int month, int year, List<LocalDate> selectedDates, Map<LocalDate, TicketType> selectedTypes) {}
