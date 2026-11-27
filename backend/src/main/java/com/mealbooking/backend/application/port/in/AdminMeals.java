package com.mealbooking.backend.application.port.in;

import com.mealbooking.backend.application.model.AdminCommands.PriceInput;
import com.mealbooking.backend.application.model.AdminResponses.KitchenCounts;
import com.mealbooking.backend.application.model.AdminResponses.Overview;
import com.mealbooking.backend.application.model.AdminResponses.PriceRow;
import com.mealbooking.backend.application.model.AdminResponses.RegistrationRow;
import java.time.LocalDate;
import java.util.List;

public interface AdminMeals {
    Overview overview();

    KitchenCounts kitchenCounts(LocalDate date);

    KitchenCounts recordKitchenCounts(LocalDate date, Long actorId);

    List<RegistrationRow> registrations(LocalDate date, Long departmentId);

    void setRegistration(Long userId, LocalDate date, boolean registered, Long actorId, String cancellationReason, Boolean withdrawal);

    List<PriceRow> prices();

    PriceRow addPrice(PriceInput input, Long actorId);
}
