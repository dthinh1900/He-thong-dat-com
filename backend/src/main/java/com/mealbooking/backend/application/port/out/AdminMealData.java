package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.application.model.AdminResponses.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AdminMealData {
    Overview overview(LocalDate today, LocalDate tomorrow, LocalDate weekStart);

    PortionCounts portionCounts(LocalDate date);

    KitchenReport lastKitchenReport(LocalDate date);

    void saveKitchenReport(LocalDate date, PortionCounts counts, Long actorId, LocalDateTime reportedAt);

    List<RegistrationState> registrationStates(LocalDate date, Long departmentId);

    void lockPricesForWrite();

    record RegistrationState(long userId, String employeeCode, String fullName, long departmentId,
        boolean active, Long ticketId, String status, long ownerId) {}
}
