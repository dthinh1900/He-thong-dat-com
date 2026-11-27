package com.mealbooking.backend.application.usecase;

import com.mealbooking.backend.application.port.in.ReadPersonalMealHistory;
import com.mealbooking.backend.application.port.out.MealHistoryReader;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.error.BusinessException;
import com.mealbooking.backend.application.model.TicketResponses.MealHistoryEntry;
import com.mealbooking.backend.application.model.TicketResponses.PersonalMealHistory;
import java.math.BigDecimal;
import java.time.YearMonth;

public final class PersonalMealHistoryUseCase implements ReadPersonalMealHistory {
    private final MealHistoryReader reader;

    public PersonalMealHistoryUseCase(MealHistoryReader reader) {
        this.reader = reader;
    }

    @Override
    public PersonalMealHistory personalMealHistory(Long ownerId, int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2200) {
            throw BusinessException.badRequest("Tháng hoặc năm không hợp lệ.");
        }
        var range = YearMonth.of(year, month);
        var rows = reader.findOwnedTickets(ownerId, range.atDay(1), range.atEndOfMonth()).stream()
            .filter(ticket -> ticket.status() != TicketStatus.WITHDRAWN)
            .map(ticket -> new MealHistoryEntry(ticket.id(), ticket.mealDate(), ticket.type(), ticket.price(),
                ticket.status(), ticket.source(), ticket.status().isBillable()))
            .toList();
        var billable = rows.stream().filter(MealHistoryEntry::billable).toList();
        var total = billable.stream().map(MealHistoryEntry::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PersonalMealHistory(month, year, billable.size(), total, rows);
    }
}
