package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.SaveMonthRegistration;
import com.mealbooking.backend.application.model.MonthRegistrationResponse;
import java.time.LocalDate;

public interface MealRegistrations {
    MonthRegistrationResponse getMonth(Long userId, int month, int year);

    MonthRegistrationResponse saveMonth(Long userId, SaveMonthRegistration request);


    void cancelTicketByAdmin(Long actorId, Long ticketId, String reason, Boolean withdrawal);

    void setDayByAdmin(Long userId, LocalDate date, boolean registered, Long actorId, String cancellationReason, Boolean withdrawal);
}
