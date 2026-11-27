package com.mealbooking.backend.application.port.out;

import com.mealbooking.backend.domain.model.MealTicket;
import com.mealbooking.backend.domain.enums.TicketStatus;
import com.mealbooking.backend.domain.enums.TicketType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealTicketStore {
    List<MealTicket> findByRegistrationId(Long registrationId);

    List<MealTicket> findByOwnerIdAndMealDateBetweenOrderByMealDate(Long ownerId, LocalDate from, LocalDate to);

    List<MealTicket> findByOwnerIdAndMealDateGreaterThanEqualAndStatusOrderByMealDateAscIdAsc(Long ownerId, LocalDate date, TicketStatus status);

    Optional<MealTicket> findForUpdate(Long id);

    List<MealTicket> findByRegistrationForUpdate(Long id);

    List<MealTicket> findAvailableForUpdate(Long ownerId, LocalDate date, TicketType type, TicketStatus status);

    <S extends MealTicket> S save(S ticket);
}
